package com.shterneregen.securelan.androidclient.call

import com.shterneregen.securelan.common.model.rtc.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CallCoordinatorTest {
    private class Media : CallMedia {
        var listener: CallMedia.Listener? = null
        var starts = 0
        var closes = 0
        var answers = 0
        var fail = false
        var offerGate: CompletableDeferred<Unit>? = null
        val ice = mutableListOf<RtcSignalEnvelope>()
        override suspend fun start(video: Boolean, listener: CallMedia.Listener) { starts++; this.listener = listener; if (fail) error("camera") }
        override suspend fun offer(): String { listener!!.ice("local", "0", 0); offerGate?.await(); return "offer" }
        override suspend fun answer(sdp: String): String { answers++; return "answer" }
        override suspend fun setAnswer(sdp: String) { answers++ }
        override fun addIce(signal: RtcSignalEnvelope) { ice += signal }
        override fun close() { closes++ }
    }
    private class Fixture(scope: TestScope) {
        val media = Media()
        val sent = mutableListOf<RtcSignalEnvelope>()
        var releases = 0
        var prepared = false
        var connected = true
        var failSend = false
        var prepareGate: CompletableDeferred<Unit>? = null
        val calls = CallCoordinator(scope.backgroundScope, { "alice" }, { connected }, {
            if (failSend) error("socket")
            sent += it
        }, media, { _, _ -> prepareGate?.await(); prepared = true }, { releases++ }, { scope.testScheduler.currentTime }, 1_000)
        fun signal(type: RtcSignalType, id: String = calls.state.value.id, peer: String = "bob", target: String = "alice", message: String = "") =
            RtcSignalEnvelope(id, peer, target, type, RtcSessionMode.AUDIO_VIDEO, "securelan-data", true, true,
                "sdp", "candidate", "0", 0, message)
        fun incoming() = calls.receive("bob", signal(RtcSignalType.OFFER, "incoming"))
    }

    @Test fun `bounded outbox cancels retired sends and a new call does not wait for stale work`() = runTest {
        val media = Media()
        val sent = mutableListOf<RtcSignalEnvelope>()
        val blocked = CompletableDeferred<Unit>()
        var block = true
        val calls = CallCoordinator(backgroundScope, { "alice" }, { true }, {
            if (block) blocked.await()
            sent += it
        }, media, { _, _ -> }, {}, { testScheduler.currentTime })
        calls.start("bob", false); runCurrent()
        val retired = calls.state.value.id
        repeat(1000) { index ->
            calls.receive("mallory", RtcSignalEnvelope("foreign-$index", "mallory", "alice", RtcSignalType.OFFER,
                RtcSessionMode.AUDIO, "data", true, false, "sdp", "", "", -1, ""))
        }
        assertTrue(calls.pendingSignalCount <= 64)
        repeat(1000) { media.listener!!.ice("candidate-$it", "0", 0) }
        assertEquals(CallPhase.ENDED, calls.state.value.phase)
        assertTrue(calls.pendingSignalCount <= 64)
        // The blocked old offer is cancelled; its pending ICE cannot delay a new offer.
        block = false
        calls.start("carol", false); runCurrent()
        assertTrue(sent.any { it.toPeer() == "carol" && it.type() == RtcSignalType.OFFER })
        assertFalse(sent.any { it.sessionId() == retired && it.type() in listOf(RtcSignalType.OFFER, RtcSignalType.ICE_CANDIDATE) })
        assertTrue(sent.any { it.sessionId() == retired && it.type() == RtcSignalType.HANGUP })
        calls.close()
    }

    @Test fun `offer precedes locally generated ice and media waits for foreground readiness`() = runTest {
        val f = Fixture(this)
        f.prepareGate = CompletableDeferred()
        f.calls.start("bob", true)
        runCurrent()
        assertEquals(0, f.media.starts)
        f.prepareGate!!.complete(Unit)
        runCurrent()
        assertTrue(f.prepared)
        assertEquals(listOf(RtcSignalType.OFFER, RtcSignalType.ICE_CANDIDATE), f.sent.map { it.type() })
        assertEquals(1, f.sent.map { it.sessionId() }.distinct().size)
    }

    @Test fun `incoming does not capture media and buffers ice until accepted`() = runTest {
        val f = Fixture(this)
        f.incoming()
        f.calls.receive("bob", f.signal(RtcSignalType.ICE_CANDIDATE))
        runCurrent()
        assertEquals(0, f.media.starts)
        assertTrue(f.media.ice.isEmpty())
        f.calls.accept("incoming", false)
        runCurrent()
        assertEquals(1, f.media.starts)
        assertEquals(1, f.media.ice.size)
        assertEquals(RtcSignalType.ANSWER, f.sent.single().type())
        f.media.listener!!.connected()
        assertEquals(CallPhase.ACTIVE, f.calls.state.value.phase)
        advanceTimeBy(2_000); runCurrent()
        assertEquals(CallPhase.ACTIVE, f.calls.state.value.phase)
    }

    @Test fun `foreign stale broadcast and forged signals cannot end current call`() = runTest {
        val f = Fixture(this)
        f.calls.start("bob", false); runCurrent()
        val id = f.calls.state.value.id
        f.calls.receive("mallory", f.signal(RtcSignalType.HANGUP))
        f.calls.receive("bob", f.signal(RtcSignalType.HANGUP, "old"))
        f.calls.receive("mallory", f.signal(RtcSignalType.HANGUP, peer = "mallory"))
        f.calls.receive("bob", f.signal(RtcSignalType.HANGUP, target = ""))
        assertEquals(CallPhase.OUTGOING, f.calls.state.value.phase)
        assertEquals(id, f.calls.state.value.id)
    }

    @Test fun `busy offer does not replace call or its timeout`() = runTest {
        val f = Fixture(this)
        f.calls.start("bob", false); runCurrent()
        val id = f.calls.state.value.id
        f.calls.receive("carol", f.signal(RtcSignalType.OFFER, "second", "carol")); runCurrent()
        assertEquals(id, f.calls.state.value.id)
        assertEquals("busy", f.sent.last().message())
        assertEquals("second", f.sent.last().sessionId())
        assertEquals("carol", f.sent.last().toPeer())
    }

    @Test fun `reject uses incoming id and cleans up once`() = runTest {
        val f = Fixture(this); f.incoming()
        f.calls.end(); f.calls.end(); runCurrent()
        assertEquals("incoming", f.sent.single().sessionId())
        assertEquals("declined", f.sent.single().message())
        assertEquals(1, f.releases)
        assertEquals(1, f.media.closes)
    }

    @Test fun `timeout closes outgoing resources and notifies peer`() = runTest {
        val f = Fixture(this); f.calls.start("bob", false); runCurrent()
        advanceTimeBy(1_001); runCurrent()
        assertEquals(CallEndReason.TIMEOUT, f.calls.state.value.reason)
        assertEquals(1, f.releases)
        assertEquals("timeout", f.sent.last().message())
    }

    @Test fun `cancel during offer prevents late offer and native callbacks from reviving call`() = runTest {
        val f = Fixture(this)
        f.media.offerGate = CompletableDeferred()
        f.calls.start("bob", true); runCurrent()
        val oldListener = f.media.listener!!
        f.calls.end(); f.media.offerGate!!.complete(Unit); runCurrent()
        assertFalse(f.sent.any { it.type() == RtcSignalType.OFFER })
        oldListener.connected()
        assertEquals(CallPhase.ENDED, f.calls.state.value.phase)
        f.calls.start("carol", false); runCurrent()
        oldListener.failed()
        assertEquals(CallPhase.OUTGOING, f.calls.state.value.phase)
    }

    @Test fun `answer flushes remote ice exactly once`() = runTest {
        val f = Fixture(this); f.calls.start("bob", false); runCurrent()
        f.calls.receive("bob", f.signal(RtcSignalType.ICE_CANDIDATE))
        assertTrue(f.media.ice.isEmpty())
        f.calls.receive("bob", f.signal(RtcSignalType.ANSWER)); runCurrent()
        f.calls.receive("bob", f.signal(RtcSignalType.ANSWER)); runCurrent()
        assertEquals(1, f.media.answers)
        assertEquals(1, f.media.ice.size)
    }

    @Test fun `end cancels an offer even if an early answer is waiting for it`() = runTest {
        val f = Fixture(this)
        f.media.offerGate = CompletableDeferred()
        f.calls.start("bob", true); runCurrent()
        f.calls.receive("bob", f.signal(RtcSignalType.ANSWER)); runCurrent()
        f.calls.end(); f.media.offerGate!!.complete(Unit); runCurrent()
        assertFalse(f.sent.any { it.type() == RtcSignalType.OFFER })
        assertEquals(0, f.media.answers)
        assertEquals(1, f.releases)
    }

    @Test fun `foreground start timeout releases resources before media is opened`() = runTest {
        val media = Media()
        var released = false
        val calls = CallCoordinator(backgroundScope, { "alice" }, { true }, {}, media,
            { _, _ -> withTimeout(100) { delay(200) } }, { released = true }, { 0 }, 1_000)
        calls.start("bob", true); runCurrent(); advanceTimeBy(101); runCurrent()
        assertEquals(CallEndReason.ERROR, calls.state.value.reason)
        assertTrue(released)
        assertEquals(0, media.starts)
    }

    @Test fun `remote end clears resources without echo and stale permission result cannot accept`() = runTest {
        val f = Fixture(this); f.incoming()
        f.calls.receive("bob", f.signal(RtcSignalType.HANGUP))
        f.calls.accept("incoming", true); runCurrent()
        assertEquals(0, f.media.starts)
        assertTrue(f.sent.isEmpty())
        assertEquals(1, f.releases)
    }

    @Test fun `media and send errors both release resources`() = runTest {
        val mediaFailure = Fixture(this)
        mediaFailure.media.fail = true
        mediaFailure.calls.start("bob", true); runCurrent()
        assertEquals(CallEndReason.ERROR, mediaFailure.calls.state.value.reason)
        assertEquals(1, mediaFailure.releases)
        val sendFailure = Fixture(this)
        sendFailure.failSend = true
        sendFailure.calls.start("bob", false); runCurrent()
        assertEquals(CallEndReason.CONNECTION_LOST, sendFailure.calls.state.value.reason)
        assertEquals(1, sendFailure.releases)
    }

    @Test fun `disconnect and peer departure end call without echo`() = runTest {
        val f = Fixture(this); f.calls.start("bob", false); runCurrent()
        val count = f.sent.size
        f.calls.peerLeft("carol")
        assertTrue(f.calls.state.value.ongoing)
        f.calls.peerLeft("BOB"); runCurrent()
        assertEquals(CallEndReason.CONNECTION_LOST, f.calls.state.value.reason)
        assertEquals(count, f.sent.size)
        f.calls.dismiss(); f.connected = false
        f.calls.start("bob", false)
        assertEquals(CallPhase.IDLE, f.calls.state.value.phase)
    }

    @Test fun `simultaneous offers resolve busy on both sides without accepting implicitly`() = runTest {
        val f = Fixture(this); f.calls.start("bob", false); runCurrent()
        f.calls.receive("bob", f.signal(RtcSignalType.OFFER, "other")); runCurrent()
        assertEquals("busy", f.sent.last().message())
        f.calls.receive("bob", f.signal(RtcSignalType.HANGUP, message = "busy"))
        assertEquals(CallEndReason.BUSY, f.calls.state.value.reason)
    }

    @Test fun `candidate flood before acceptance is bounded and terminates the call`() = runTest {
        val f = Fixture(this); f.incoming()
        repeat(257) { f.calls.receive("bob", f.signal(RtcSignalType.ICE_CANDIDATE)) }
        assertEquals(CallEndReason.ERROR, f.calls.state.value.reason)
        assertEquals(0, f.media.starts)
    }

    @Test fun `wire codec preserves SDP multiline unicode and candidate fields`() {
        val original = RtcSignalEnvelope("id", "Алиса", "Боб", RtcSignalType.OFFER, RtcSessionMode.AUDIO_VIDEO,
            "securelan-data", true, true, "v=0\r\na=fingerprint:sha-256 test;value\r\n", "candidate:one", "0", 0, "")
        assertEquals(original, RtcSignalCodec.deserialize(RtcSignalCodec.serialize(original)))
    }
}
