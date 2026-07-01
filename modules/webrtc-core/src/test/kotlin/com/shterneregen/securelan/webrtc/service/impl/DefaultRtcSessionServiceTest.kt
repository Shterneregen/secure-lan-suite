package com.shterneregen.securelan.webrtc.service.impl

import com.shterneregen.securelan.common.model.rtc.RtcSessionMode
import com.shterneregen.securelan.common.model.rtc.RtcSessionState
import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope
import com.shterneregen.securelan.common.model.rtc.RtcSignalType
import com.shterneregen.securelan.webrtc.event.RtcEvent
import com.shterneregen.securelan.webrtc.event.RtcRuntimeWarningEvent
import com.shterneregen.securelan.webrtc.event.RtcStateChangedEvent
import com.shterneregen.securelan.webrtc.runtime.RtcEngine
import com.shterneregen.securelan.webrtc.runtime.RtcRuntimeStatus
import com.shterneregen.securelan.webrtc.service.RtcSessionRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer

class DefaultRtcSessionServiceTest {
    @Test
    fun shouldStartSessionAndMarkUnavailableWhenEngineIsUnavailable() {
        val events = CopyOnWriteArrayList<RtcEvent>()
        val signals = CopyOnWriteArrayList<RtcSignalEnvelope>()
        val service = DefaultRtcSessionService(
            events::add,
            signals::add,
            FakeRtcEngine(RtcRuntimeStatus.unavailable("rtc unavailable")),
        )

        val snapshot = service.startSession(
            RtcSessionRequest(" local ", " remote ", RtcSessionMode.AUDIO, " label "),
        )

        assertEquals(RtcSessionState.NEGOTIATING, snapshot.state)
        assertTrue(waitUntil { events.any { it is RtcRuntimeWarningEvent && it.message == "rtc unavailable" } })
        assertEquals(RtcSessionState.UNAVAILABLE, service.currentSession().orElseThrow().state)
        assertTrue(signals.isEmpty())
    }

    @Test
    fun shouldIgnoreNullInboundSignalAndNonTargetedSignal() {
        val events = CopyOnWriteArrayList<RtcEvent>()
        val service = DefaultRtcSessionService(
            events::add,
            { },
            FakeRtcEngine(RtcRuntimeStatus("fake", true, "ok")),
        )

        service.acceptInboundSignal("local", null)
        service.acceptInboundSignal(
            "local",
            RtcSignalEnvelope.offer("remote", "someone-else", RtcSessionMode.DATA, "securelan-data", "sdp"),
        )

        assertTrue(events.isEmpty())
        assertFalse(service.currentSession().isPresent)
    }

    @Test
    fun shouldCloseCurrentSessionWithHangupSignalAndClosedState() {
        val events = CopyOnWriteArrayList<RtcEvent>()
        val signals = CopyOnWriteArrayList<RtcSignalEnvelope>()
        val engine = FakeRtcEngine(RtcRuntimeStatus("fake", true, "ok"))
        val service = DefaultRtcSessionService(events::add, signals::add, engine)
        service.startSession(RtcSessionRequest("local", "remote", RtcSessionMode.DATA, "securelan-data"))

        service.closeCurrentSession()

        assertEquals(1, signals.size)
        assertEquals("local", signals.single().fromPeer())
        assertEquals("remote", signals.single().toPeer())
        assertEquals("Session closed by local peer", signals.single().message())
        assertEquals(RtcSessionState.CLOSED, service.currentSession().orElseThrow().state)
        assertTrue(events.filterIsInstance<RtcStateChangedEvent>().any { it.state == RtcSessionState.CLOSING })
        assertTrue(events.filterIsInstance<RtcStateChangedEvent>().any { it.state == RtcSessionState.CLOSED })
        assertTrue(waitUntil { engine.closedSessionIds.isNotEmpty() })
    }

    @Test
    fun shouldCloseExistingSessionBeforeStartingAnotherCall() {
        val events = CopyOnWriteArrayList<RtcEvent>()
        val signals = CopyOnWriteArrayList<RtcSignalEnvelope>()
        val engine = FakeRtcEngine(RtcRuntimeStatus("fake", true, "ok"))
        val service = DefaultRtcSessionService(events::add, signals::add, engine)
        val first = service.startSession(RtcSessionRequest("local", "remote-a", RtcSessionMode.AUDIO, "securelan-data"))

        val second = service.startSession(RtcSessionRequest("local", "remote-b", RtcSessionMode.AUDIO_VIDEO, "securelan-data"))

        assertEquals("remote-b", second.remotePeer)
        assertTrue(signals.any { it.type() == RtcSignalType.HANGUP && it.sessionId() == first.sessionId })
        assertTrue(waitUntil { engine.closedSessionIds.contains(first.sessionId) })
        assertEquals(second.sessionId, service.currentSession().orElseThrow().sessionId)
    }

    @Test
    fun shouldKeepClosedStateWhenEngineEmitsLateStateAfterHangup() {
        val events = CopyOnWriteArrayList<RtcEvent>()
        val signals = CopyOnWriteArrayList<RtcSignalEnvelope>()
        val engine = FakeRtcEngine(RtcRuntimeStatus("fake", true, "ok"), emitLateConnectingOnClose = true)
        val service = DefaultRtcSessionService(events::add, signals::add, engine)
        service.startSession(RtcSessionRequest("local", "remote", RtcSessionMode.AUDIO_VIDEO, "securelan-data"))

        service.closeCurrentSession()

        assertTrue(waitUntil { engine.closedSessionIds.isNotEmpty() })
        assertEquals(RtcSessionState.CLOSED, service.currentSession().orElseThrow().state)
        assertTrue(events.filterIsInstance<RtcStateChangedEvent>().any { it.state == RtcSessionState.CONNECTING && it.message == "Late fake engine state" })
    }

    @Test
    fun shouldCloseEngineAndStateWhenRemoteHangupArrives() {
        val events = CopyOnWriteArrayList<RtcEvent>()
        val signals = CopyOnWriteArrayList<RtcSignalEnvelope>()
        val engine = FakeRtcEngine(RtcRuntimeStatus("fake", true, "ok"))
        val service = DefaultRtcSessionService(events::add, signals::add, engine)
        val started = service.startSession(RtcSessionRequest("local", "remote", RtcSessionMode.AUDIO_VIDEO, "securelan-data"))

        service.acceptInboundSignal(
            "local",
            RtcSignalEnvelope.hangup(started.sessionId ?: "", "remote", "local", RtcSessionMode.AUDIO_VIDEO, "securelan-data", "Remote ended"),
        )

        assertTrue(waitUntil { engine.closedSessionIds.contains(started.sessionId) })
        assertEquals(RtcSessionState.CLOSED, service.currentSession().orElseThrow().state)
        assertTrue(events.filterIsInstance<RtcStateChangedEvent>().any { it.state == RtcSessionState.CLOSED && it.message == "Remote ended" })
    }

    private class FakeRtcEngine(
        private val status: RtcRuntimeStatus,
        private val emitLateConnectingOnClose: Boolean = false,
    ) : RtcEngine {
        val closedSessionIds = CopyOnWriteArrayList<String?>()

        override fun status(): RtcRuntimeStatus = status

        override fun startSession(
            sessionId: String?,
            localPeer: String?,
            remotePeer: String?,
            mode: RtcSessionMode?,
            dataChannelLabel: String?,
            audioCaptureDeviceId: String?,
            videoCaptureDeviceId: String?,
            outboundSignalConsumer: Consumer<RtcSignalEnvelope>,
            eventConsumer: Consumer<RtcEvent>,
        ) {
        }

        override fun handleRemoteSignal(
            signal: RtcSignalEnvelope,
            outboundSignalConsumer: Consumer<RtcSignalEnvelope>,
            eventConsumer: Consumer<RtcEvent>,
        ) {
        }

        override fun sendData(sessionId: String?, payload: String?, eventConsumer: Consumer<RtcEvent>) {
        }

        override fun closeSession(sessionId: String?, eventConsumer: Consumer<RtcEvent>) {
            closedSessionIds.add(sessionId)
            if (emitLateConnectingOnClose) {
                eventConsumer.accept(RtcStateChangedEvent(sessionId, "remote", RtcSessionMode.AUDIO_VIDEO, RtcSessionState.CONNECTING, "Late fake engine state"))
            }
        }
    }

    private companion object {
        private fun waitUntil(condition: () -> Boolean): Boolean {
            repeat(50) {
                if (condition()) {
                    return true
                }
                Thread.sleep(20)
            }
            return condition()
        }
    }
}
