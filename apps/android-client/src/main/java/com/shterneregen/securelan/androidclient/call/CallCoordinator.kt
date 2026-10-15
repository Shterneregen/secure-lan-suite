package com.shterneregen.securelan.androidclient.call

import com.shterneregen.securelan.common.model.rtc.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class CallPhase { IDLE, INCOMING, OUTGOING, CONNECTING, ACTIVE, ENDED }
enum class CallEndReason { COMPLETED, DECLINED, BUSY, TIMEOUT, CONNECTION_LOST, ERROR }
/** Call snapshot; [startedAt] is a monotonic clock value set only when media becomes active. */
data class CallState(
    val phase: CallPhase = CallPhase.IDLE,
    val id: String = "",
    val peer: String = "",
    val video: Boolean = false,
    val startedAt: Long = 0,
    val reason: CallEndReason? = null,
) {
    val ongoing: Boolean get() = phase != CallPhase.IDLE && phase != CallPhase.ENDED
}

/** All methods and callbacks run on the owning (main) dispatcher. Native callbacks must dispatch to it. */
interface CallMedia {
    suspend fun start(video: Boolean, listener: Listener)
    suspend fun offer(): String
    suspend fun answer(sdp: String): String
    suspend fun setAnswer(sdp: String)
    fun addIce(signal: RtcSignalEnvelope)
    fun close()
    interface Listener {
        fun ice(candidate: String, mid: String, index: Int)
        fun connected()
        fun failed()
    }
}

/**
 * Coordinates one call on the owning main dispatcher without capturing media for an incoming offer.
 * [prepare] must establish foreground/permission readiness before media starts; [release] restores
 * platform resources on termination. Signaling is ordered and correlated by session and peer.
 * [now] supplies a monotonic clock, and stale callbacks cannot revive a finished call.
 */
class CallCoordinator(
    private val scope: CoroutineScope,
    private val nickname: () -> String,
    private val available: () -> Boolean,
    private val send: suspend (RtcSignalEnvelope) -> Unit,
    private val media: CallMedia,
    private val prepare: suspend (String, Boolean) -> Unit,
    private val release: () -> Unit,
    private val now: () -> Long,
    private val timeoutMillis: Long = 45_000,
) {
    private val mutableState = MutableStateFlow(CallState())
    val state = mutableState.asStateFlow()
    private var negotiation: Job? = null
    private val operations = mutableSetOf<Job>()
    private var timeout: Job? = null
    private var incoming: RtcSignalEnvelope? = null
    private var remoteReady = false
    private var localReady = false
    private val remoteIce = mutableListOf<RtcSignalEnvelope>()
    private val localIce = mutableListOf<RtcSignalEnvelope>()
    private data class PendingSignal(val signal: RtcSignalEnvelope, val generation: Long, val terminal: Boolean = false, val reply: Boolean = false)
    private val outbox = ArrayDeque<PendingSignal>()
    private val wakeSender = Channel<Unit>(Channel.CONFLATED)
    private var generation = 0L
    private var lastReplyAt: Long? = null
    private var inFlight: Pair<PendingSignal, Job>? = null
    private val mutableSignalingError = MutableStateFlow<String?>(null)
    val signalingError = mutableSignalingError.asStateFlow()
    internal val pendingSignalCount: Int get() = outbox.size
    private val sender = scope.launch {
        for (wake in wakeSender) {
            while (outbox.isNotEmpty()) {
                val work = outbox.removeFirst()
                if (!work.terminal && (work.generation != generation || (!work.reply &&
                    (!state.value.ongoing || state.value.id != work.signal.sessionId())))) continue
                val task = scope.launch(start = CoroutineStart.LAZY) {
                    try { withTimeout(if (work.terminal || work.reply) 1_000 else 10_000) { send(work.signal) } }
                    catch (_: TimeoutCancellationException) { sendFailed(work) }
                    catch (e: CancellationException) { throw e }
                    catch (_: Exception) { sendFailed(work) }
                }
                inFlight = work to task
                task.start()
                task.join()
                inFlight = null
            }
        }
    }

    private fun sendFailed(work: PendingSignal) {
        if (work.terminal) mutableSignalingError.value = "The peer could not be notified that the call ended."
        else if (!work.reply && work.generation == generation) transportFailed(work.signal)
    }

    private fun retirePending() {
        generation++
        outbox.removeAll { !it.terminal }
        inFlight?.takeUnless { it.first.terminal }?.second?.cancel()
    }

    private fun reply(signal: RtcSignalEnvelope) {
        val timestamp = now()
        if (lastReplyAt?.let { timestamp - it < 1_000 } == true) return
        lastReplyAt = timestamp
        if (outbox.size < 64 && outbox.none { it.reply }) {
            outbox.addLast(PendingSignal(signal, generation, reply = true))
            wakeSender.trySend(Unit)
        }
    }

    fun start(peer: String, video: Boolean) {
        if (state.value.ongoing || !available() || peer.isBlank() || peer.equals(nickname(), true)) return
        begin(CallState(CallPhase.OUTGOING, UUID.randomUUID().toString(), peer, video))
        negotiate {
            prepare(it.id, video)
            media.start(video, listener(it))
            val sdp = media.offer()
            emit(envelope(it, RtcSignalType.OFFER, sdp = sdp))
            flushLocal()
        }
    }

    /** Accepts only the current offer; [sendVideo] allows answering a video offer with audio only. */
    fun accept(id: String, sendVideo: Boolean) {
        val offer = incoming ?: return
        val current = state.value
        if (current.phase != CallPhase.INCOMING || current.id != id) return
        mutableState.value = current.copy(phase = CallPhase.CONNECTING)
        armTimeout(current.id)
        negotiate {
            prepare(it.id, sendVideo && it.video)
            media.start(sendVideo && it.video, listener(it))
            val sdp = media.answer(offer.sdp())
            flushRemote()
            emit(envelope(it, RtcSignalType.ANSWER, sdp = sdp))
            flushLocal()
        }
    }

    /** Handles a signal whose [sender] comes from the authenticated chat transport. */
    fun receive(sender: String, signal: RtcSignalEnvelope) {
        if (!available() || signal.sessionId().isBlank() || signal.toPeer().isBlank() ||
            !signal.toPeer().equals(nickname(), true) || !signal.fromPeer().equals(sender, true) ||
            sender.equals(nickname(), true)) return
        val current = state.value
        if (signal.type() == RtcSignalType.OFFER) {
            if (current.ongoing && current.id == signal.sessionId() && current.peer.equals(sender, true)) return
            if (current.ongoing) {
                reply(RtcSignalEnvelope.hangup(signal.sessionId(), nickname(), sender, signal.mode(), "securelan-data", "busy"))
            } else if (signal.sdp().isNotBlank() && signal.mode() in setOf(RtcSessionMode.AUDIO, RtcSessionMode.AUDIO_VIDEO)) {
                begin(CallState(CallPhase.INCOMING, signal.sessionId(), sender, signal.mode().videoEnabled()))
                incoming = signal
            } else {
                reply(RtcSignalEnvelope.error(signal.sessionId(), nickname(), sender, signal.mode(), "securelan-data", "unsupported"))
            }
            return
        }
        if (!current.ongoing || current.id != signal.sessionId() || !current.peer.equals(sender, true)) return
        when (signal.type()) {
            RtcSignalType.ANSWER -> if (current.phase == CallPhase.OUTGOING && signal.sdp().isNotBlank()) {
                mutableState.value = current.copy(phase = CallPhase.CONNECTING)
                armTimeout(current.id)
                // The offer operation may still be returning from setLocalDescription.
                val previous = negotiation
                negotiate {
                    previous?.join()
                    media.setAnswer(signal.sdp())
                    flushRemote()
                }
            }
            RtcSignalType.ICE_CANDIDATE -> if (signal.iceCandidate().isNotBlank() && signal.sdpMLineIndex() >= 0) {
                if (remoteReady) runCatching { media.addIce(signal) }.onFailure { finish(CallEndReason.ERROR) }
                else if (remoteIce.size < 256) remoteIce += signal
                else finish(CallEndReason.ERROR)
            }
            RtcSignalType.HANGUP -> finish(when (signal.message()) {
                "busy" -> CallEndReason.BUSY
                "declined" -> CallEndReason.DECLINED
                "timeout" -> CallEndReason.TIMEOUT
                else -> CallEndReason.COMPLETED
            }, notify = false)
            RtcSignalType.ERROR -> finish(CallEndReason.ERROR, notify = false)
            else -> Unit
        }
    }

    fun end() = finish(if (state.value.phase == CallPhase.INCOMING) CallEndReason.DECLINED else CallEndReason.COMPLETED)
    fun connectionLost() = finish(CallEndReason.CONNECTION_LOST, notify = false)
    fun peerLeft(peer: String) { if (state.value.peer.equals(peer, true)) connectionLost() }
    fun dismiss() { if (!state.value.ongoing) mutableState.value = CallState() }
    fun close() { connectionLost(); outbox.clear(); wakeSender.close(); inFlight?.second?.cancel(); sender.cancel() }

    private fun begin(value: CallState) {
        retirePending()
        mutableSignalingError.value = null
        remoteReady = false; localReady = false
        remoteIce.clear(); localIce.clear(); incoming = null
        mutableState.value = value
        armTimeout(value.id)
    }

    private fun negotiate(block: suspend (CallState) -> Unit) {
        val captured = state.value
        val task = scope.launch(start = CoroutineStart.LAZY) {
            try { block(captured) }
            catch (_: TimeoutCancellationException) { if (matches(captured)) finish(CallEndReason.ERROR) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (matches(captured)) finish(CallEndReason.ERROR) }
            finally { operations.remove(coroutineContext[Job]) }
        }
        negotiation = task
        operations += task
        task.start()
    }

    private fun listener(call: CallState) = object : CallMedia.Listener {
        override fun ice(candidate: String, mid: String, index: Int) {
            if (!matches(call)) return
            val signal = RtcSignalEnvelope.iceCandidate(call.id, nickname(), call.peer, mode(call), "securelan-data", candidate, mid, index)
            if (localReady) emit(signal) else if (localIce.size < 256) localIce += signal
        }
        override fun connected() {
            if (!matches(call)) return
            timeout?.cancel()
            if (state.value.phase != CallPhase.ACTIVE) mutableState.value = state.value.copy(phase = CallPhase.ACTIVE, startedAt = now())
        }
        override fun failed() { if (matches(call)) finish(CallEndReason.CONNECTION_LOST) }
    }

    private fun flushLocal() { localReady = true; val pending = localIce.toList(); localIce.clear(); pending.forEach(::emit) }
    private fun flushRemote() { remoteReady = true; remoteIce.forEach(media::addIce); remoteIce.clear() }
    private fun matches(call: CallState) = state.value.ongoing && state.value.id == call.id
    private fun armTimeout(id: String) {
        timeout?.cancel()
        timeout = scope.launch { delay(timeoutMillis); if (state.value.id == id) finish(CallEndReason.TIMEOUT) }
    }
    private fun transportFailed(signal: RtcSignalEnvelope) {
        if (state.value.id == signal.sessionId()) finish(CallEndReason.CONNECTION_LOST, notify = false)
    }
    private fun finish(reason: CallEndReason, notify: Boolean = true) {
        val current = state.value
        if (!current.ongoing) return
        mutableState.value = current.copy(phase = CallPhase.ENDED, reason = reason)
        retirePending()
        operations.toList().forEach { it.cancel() }; operations.clear(); negotiation = null
        timeout?.cancel(); timeout = null
        incoming = null; remoteIce.clear(); localIce.clear()
        if (notify) emitTerminal(envelope(current, RtcSignalType.HANGUP, message = when (reason) {
            CallEndReason.DECLINED -> "declined"
            CallEndReason.TIMEOUT -> "timeout"
            else -> reason.name.lowercase()
        }))
        try { media.close() } finally { release() }
    }
    private fun emit(signal: RtcSignalEnvelope) {
        if (!state.value.ongoing || state.value.id != signal.sessionId()) return
        if (outbox.size >= 64) { finish(CallEndReason.ERROR); return }
        outbox.addLast(PendingSignal(signal, generation))
        wakeSender.trySend(Unit)
    }
    private fun emitTerminal(signal: RtcSignalEnvelope) {
        if (outbox.size >= 64) {
            mutableSignalingError.value = "The peer could not be notified that the call ended."
            return
        }
        outbox.addLast(PendingSignal(signal, generation, terminal = true))
        wakeSender.trySend(Unit)
    }
    private fun mode(call: CallState) = if (call.video) RtcSessionMode.AUDIO_VIDEO else RtcSessionMode.AUDIO
    private fun envelope(call: CallState, type: RtcSignalType, sdp: String = "", message: String = "") =
        RtcSignalEnvelope(call.id, nickname(), call.peer, type, mode(call), "securelan-data", true, call.video, sdp, "", "", -1, message)
}
