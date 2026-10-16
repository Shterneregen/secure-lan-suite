package com.shterneregen.securelan.webrtc.service.impl

import com.shterneregen.securelan.common.model.rtc.*
import com.shterneregen.securelan.webrtc.event.*
import com.shterneregen.securelan.webrtc.runtime.*
import com.shterneregen.securelan.webrtc.service.*
import java.util.Optional
import java.util.UUID
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

/** Single-call FSM with explicit inbound consent, peer binding and serialized bounded native IO. */
open class DefaultRtcSessionService : RtcSessionService {
    private val eventPublisher: RtcEventPublisher
    private val signalingGateway: RtcSignalingGateway
    private val rtcExecutor = ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, ArrayBlockingQueue(128),
        { task -> Thread(task, "securelan-rtc").apply { isDaemon = true } })
    private val timers = ScheduledThreadPoolExecutor(1) { task ->
        Thread(task, "securelan-rtc-timeout").apply { isDaemon = true }
    }.apply { removeOnCancelPolicy = true }
    private val current = AtomicReference<RtcSessionSnapshot?>()
    @Volatile private var engine: RtcEngine? = null
    private val retiredSessions = linkedSetOf<String>()
    private var pendingOffer: RtcSignalEnvelope? = null
    private val pendingIce = mutableListOf<RtcSignalEnvelope>()
    private var setupTimer: ScheduledFuture<*>? = null
    private var outgoing = false
    private var answerReceived = false
    private var candidateCount = 0
    @Volatile private var closed = false
    @Volatile private var mediaConfiguration = RtcMediaConfiguration()
    private var appliedConfiguration: RtcMediaConfiguration? = null
    private var ownsEngine = true
    private var engineFactory: (RtcMediaConfiguration) -> RtcEngine = RtcEngineProvider::createDefault
    private val signalExecutor = ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, ArrayBlockingQueue(128),
        { task -> Thread(task, "securelan-rtc-signaling").apply { isDaemon = true } })
    private var setupTimeoutMillis = 30_000L

    constructor(eventPublisher: RtcEventPublisher, signalingGateway: RtcSignalingGateway) {
        this.eventPublisher = eventPublisher
        this.signalingGateway = signalingGateway
    }

    constructor(eventPublisher: RtcEventPublisher, signalingGateway: RtcSignalingGateway, engineFactory: (RtcMediaConfiguration) -> RtcEngine) :
        this(eventPublisher, signalingGateway) { this.engineFactory = engineFactory }

    constructor(eventPublisher: RtcEventPublisher, signalingGateway: RtcSignalingGateway, rtcEngine: RtcEngine) :
        this(eventPublisher, signalingGateway) { engine = rtcEngine; ownsEngine = false }

    constructor(eventPublisher: RtcEventPublisher, signalingGateway: RtcSignalingGateway, rtcEngine: RtcEngine, setupTimeoutMillis: Long) :
        this(eventPublisher, signalingGateway, rtcEngine) {
        require(setupTimeoutMillis in 1..300_000)
        this.setupTimeoutMillis = setupTimeoutMillis
    }

    override fun configureMediaDevices(configuration: RtcMediaConfiguration) { mediaConfiguration = configuration }

    override fun runtimeStatus(): RtcRuntimeStatus = engine?.status() ?: RtcRuntimeStatus.unavailable("RTC initializes after a call is accepted.")
    override fun currentSession(): Optional<RtcSessionSnapshot> = Optional.ofNullable(current.get())

    @Synchronized
    override fun startSession(request: RtcSessionRequest): RtcSessionSnapshot {
        check(!closed) { "RTC service is closed" }
        require(request.localPeer().isNotBlank() && request.remotePeer().isNotBlank()) { "Connect chat and select a peer first" }
        if (active(current.get())) closeCurrentSession()
        val configured = mediaConfiguration
        val devices = configured.copy(microphoneId = request.audioCaptureDeviceId().ifBlank { configured.microphoneId },
            cameraId = request.videoCaptureDeviceId().ifBlank { configured.cameraId })
        val snapshot = RtcSessionSnapshot(UUID.randomUUID().toString(), request.localPeer(), request.remotePeer(),
            request.mode(), request.dataChannelLabel(), RtcSessionState.NEGOTIATING, "Preparing call")
        outgoing = true
        answerReceived = false
        candidateCount = 0
        publish(snapshot)
        armSetupTimeout(snapshot.sessionId!!)
        execute {
            if (!matches(snapshot.sessionId)) return@execute
            val runtime = ensureEngine(devices)
            if (!runtime.status().available) { failUnavailable(snapshot.sessionId, runtime.status().message); return@execute }
            runtime.startSession(snapshot.sessionId, snapshot.localPeer, snapshot.remotePeer, snapshot.mode,
                snapshot.dataChannelLabel, request.audioCaptureDeviceId(), request.videoCaptureDeviceId(),
                Consumer(::sendSignal), Consumer(::forwardEvent))
        }
        return snapshot
    }

    @Synchronized
    override fun acceptInboundSignal(localPeer: String?, signal: RtcSignalEnvelope?) {
        if (closed || localPeer.isNullOrBlank() || signal == null || signal.toPeer().isBlank() ||
            !signal.toPeer().equals(localPeer, true) || signal.fromPeer().isBlank() ||
            signal.sessionId().length !in 1..128 || signal.sdp().length > 48_000 ||
            signal.iceCandidate().length > 4096 || signal.dataChannelLabel().length > 128) return
        if (signal.sessionId() in retiredSessions) return
        val existing = current.get()
        if (signal.type() == RtcSignalType.OFFER) {
            if (active(existing)) {
                if (existing!!.sessionId != signal.sessionId()) sendControl(RtcSignalEnvelope.hangup(
                    signal.sessionId(), localPeer, signal.fromPeer(), signal.mode(), signal.dataChannelLabel(), "Busy"))
                return
            }
            outgoing = false
            answerReceived = false
            candidateCount = 0
            pendingIce.clear()
            pendingOffer = signal
            publish(RtcSessionSnapshot(signal.sessionId(), localPeer, signal.fromPeer(), signal.mode(),
                signal.dataChannelLabel(), RtcSessionState.INCOMING, "Incoming call from " + signal.fromPeer()))
            armSetupTimeout(signal.sessionId())
            return
        }
        if (!active(existing) || existing!!.sessionId != signal.sessionId() ||
            existing.remotePeer != signal.fromPeer() || existing.mode != signal.mode() ||
            existing.dataChannelLabel != signal.dataChannelLabel()) return
        when (signal.type()) {
            RtcSignalType.HANGUP, RtcSignalType.ERROR -> finish(existing, signal.message().ifBlank { "Remote peer ended the call" }, false)
            RtcSignalType.ANSWER -> {
                if (!outgoing || answerReceived || existing.state !in setOf(RtcSessionState.NEGOTIATING, RtcSessionState.CONNECTING)) return
                answerReceived = true
                dispatch(signal)
            }
            RtcSignalType.ICE_CANDIDATE -> {
                if (++candidateCount > 256) { finish(existing, "Too many ICE candidates", true); return }
                if (existing.state == RtcSessionState.INCOMING) pendingIce.add(signal) else dispatch(signal)
            }
            else -> Unit
        }
    }

    @Synchronized
    override fun acceptIncomingSession(sessionId: String): Boolean {
        val snapshot = current.get() ?: return false
        val offer = pendingOffer ?: return false
        if (closed || snapshot.sessionId != sessionId || snapshot.state != RtcSessionState.INCOMING) return false
        pendingOffer = null
        val devices = mediaConfiguration
        val candidates = pendingIce.toList()
        pendingIce.clear()
        publish(withState(snapshot, RtcSessionState.NEGOTIATING, "Accepted incoming call"))
        execute {
            if (!matches(sessionId)) return@execute
            val runtime = ensureEngine(devices)
            if (!runtime.status().available) { failUnavailable(sessionId, runtime.status().message); return@execute }
            runtime.handleRemoteSignal(offer, Consumer(::sendSignal), Consumer(::forwardEvent))
            candidates.forEach { if (matches(sessionId)) runtime.handleRemoteSignal(it, Consumer(::sendSignal), Consumer(::forwardEvent)) }
        }
        return true
    }

    private fun dispatch(signal: RtcSignalEnvelope) = execute {
        if (matches(signal.sessionId())) engine?.handleRemoteSignal(signal, Consumer(::sendSignal), Consumer(::forwardEvent))
    }

    @Synchronized
    override fun sendDataMessage(payload: String?) {
        val snapshot = current.get() ?: return
        if (snapshot.state != RtcSessionState.CONNECTED || payload == null || payload.toByteArray(Charsets.UTF_8).size > 64 * 1024) return
        execute { if (matches(snapshot.sessionId)) engine?.sendData(snapshot.sessionId, payload, Consumer(::forwardEvent)) }
    }

    @Synchronized
    override fun closeCurrentSession() {
        val snapshot = current.get() ?: return
        if (active(snapshot)) finish(snapshot, "Session closed by local peer", true)
    }

    @Synchronized
    private fun finish(snapshot: RtcSessionSnapshot, reason: String, notify: Boolean) {
        retire(snapshot.sessionId)
        setupTimer?.cancel(false)
        pendingOffer = null
        pendingIce.clear()
        publish(withState(snapshot, RtcSessionState.CLOSING, reason))
        if (notify) sendControl(RtcSignalEnvelope.hangup(snapshot.sessionId.orEmpty(), snapshot.localPeer.orEmpty(), snapshot.remotePeer.orEmpty(),
            snapshot.mode ?: RtcSessionMode.DATA, snapshot.dataChannelLabel.orEmpty(), reason))
        publish(withState(snapshot, RtcSessionState.CLOSED, reason))
        execute { engine?.closeSession(snapshot.sessionId, Consumer(::forwardEvent)) }
    }

    private fun armSetupTimeout(id: String) {
        setupTimer?.cancel(false)
        setupTimer = timers.schedule({
            synchronized(this) {
                val snapshot = current.get()
                if (snapshot?.sessionId == id && snapshot.state != RtcSessionState.CONNECTED && active(snapshot)) finish(snapshot, "Call timed out", true)
            }
        }, setupTimeoutMillis, TimeUnit.MILLISECONDS)
    }

    @Synchronized
    override fun close() {
        if (closed) return
        closeCurrentSession()
        closed = true
        timers.shutdownNow()
        execute { engine?.close() }
        rtcExecutor.shutdown()
        signalExecutor.shutdown()
    }

    @Synchronized
    private fun forwardEvent(event: RtcEvent) {
        val snapshot = current.get()
        if (event is RtcStateChangedEvent) {
            if (!active(snapshot) || snapshot!!.sessionId != event.sessionId || snapshot.remotePeer != event.remotePeer) return
            if (event.state == RtcSessionState.CONNECTED) setupTimer?.cancel(false)
            current.set(withState(snapshot, event.state, event.message))
            if (!active(current.get())) { retire(snapshot.sessionId); execute { engine?.closeSession(snapshot.sessionId, Consumer(::forwardEvent)) } }
        } else {
            // Native callbacks from a previous/closed room must not restore stale media state.
            val id = when (event) {
                is RtcDataMessageEvent -> event.sessionId
                is RtcAudioLevelEvent -> event.sessionId
                is RtcVideoFrameEvent -> event.sessionId()
                else -> null
            }
            if (id != null && (!active(snapshot) || snapshot?.sessionId != id)) return
        }
        eventPublisher.publish(event)
    }

    @Synchronized private fun sendSignal(signal: RtcSignalEnvelope) {
        val snapshot = current.get() ?: return
        if (!active(snapshot) || snapshot.sessionId != signal.sessionId() || snapshot.remotePeer != signal.toPeer() || snapshot.localPeer != signal.fromPeer()) return
        sendControl(signal)
    }
    private fun sendControl(signal: RtcSignalEnvelope) {
        val queuedAt = System.nanoTime()
        val terminal = signal.type() in setOf(RtcSignalType.HANGUP, RtcSignalType.ERROR)
        try {
            signalExecutor.execute {
                if (System.nanoTime() - queuedAt > (if (terminal) 1_000_000_000L else 10_000_000_000L)) return@execute
                if (!terminal && !matches(signal.sessionId())) return@execute
                runCatching { signalingGateway.send(signal) }
            }
        } catch (_: RejectedExecutionException) { eventPublisher.publish(RtcRuntimeWarningEvent("Realtime signaling queue unavailable")) }
    }
    private fun retire(id: String?) {
        if (id != null) retiredSessions.add(id)
        while (retiredSessions.size > 256) retiredSessions.remove(retiredSessions.first())
    }
    private fun matches(id: String?): Boolean = current.get().let { it?.sessionId == id && active(it) }
    private fun active(snapshot: RtcSessionSnapshot?): Boolean = snapshot != null && snapshot.state !in
        setOf(RtcSessionState.IDLE, RtcSessionState.CLOSED, RtcSessionState.CLOSING, RtcSessionState.FAILED, RtcSessionState.UNAVAILABLE)
    private fun ensureEngine(configuration: RtcMediaConfiguration): RtcEngine {
        if (ownsEngine && (appliedConfiguration != configuration || engine?.status()?.available == false)) {
            engine?.close()
            engine = null
        }
        return engine ?: engineFactory(configuration).also { engine = it; appliedConfiguration = configuration }
    }
    private fun publish(snapshot: RtcSessionSnapshot) {
        current.set(snapshot)
        eventPublisher.publish(RtcStateChangedEvent(snapshot.sessionId, snapshot.remotePeer, snapshot.mode, snapshot.state, snapshot.message))
    }
    private fun withState(s: RtcSessionSnapshot, state: RtcSessionState?, message: String?) =
        RtcSessionSnapshot(s.sessionId, s.localPeer, s.remotePeer, s.mode, s.dataChannelLabel, state, message)
    @Synchronized private fun failUnavailable(id: String?, message: String?) {
        val snapshot = current.get() ?: return
        if (snapshot.sessionId == id && active(snapshot)) publish(withState(snapshot, RtcSessionState.UNAVAILABLE, message))
        eventPublisher.publish(RtcRuntimeWarningEvent(message))
    }
    private fun execute(action: () -> Unit) {
        try { rtcExecutor.execute { try { action() } catch (_: Exception) {
            synchronized(this) { current.get()?.takeIf(::active)?.let { finish(it, "RTC operation failed", true) } }
            eventPublisher.publish(RtcRuntimeWarningEvent("RTC operation failed"))
        } } }
        catch (_: RejectedExecutionException) {
            if (!rtcExecutor.isShutdown) synchronized(this) {
                current.get()?.let { retire(it.sessionId); publish(withState(it, RtcSessionState.FAILED, "RTC operation queue is full")) }
                pendingOffer = null
                pendingIce.clear()
                setupTimer?.cancel(false)
                rtcExecutor.queue.clear()
                runCatching { rtcExecutor.execute { runCatching { engine?.close() }; engine = null } }
            }
        }
    }
}
