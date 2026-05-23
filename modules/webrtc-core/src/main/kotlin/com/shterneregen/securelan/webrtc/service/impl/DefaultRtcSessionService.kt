package com.shterneregen.securelan.webrtc.service.impl

import com.shterneregen.securelan.common.model.rtc.RtcSessionMode
import com.shterneregen.securelan.common.model.rtc.RtcSessionState
import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope
import com.shterneregen.securelan.common.model.rtc.RtcSignalType
import com.shterneregen.securelan.webrtc.event.RtcDataMessageEvent
import com.shterneregen.securelan.webrtc.event.RtcEvent
import com.shterneregen.securelan.webrtc.event.RtcRuntimeWarningEvent
import com.shterneregen.securelan.webrtc.event.RtcStateChangedEvent
import com.shterneregen.securelan.webrtc.runtime.NoOpRtcEngine
import com.shterneregen.securelan.webrtc.runtime.RtcEngine
import com.shterneregen.securelan.webrtc.runtime.RtcEngineProvider
import com.shterneregen.securelan.webrtc.runtime.RtcRuntimeStatus
import com.shterneregen.securelan.webrtc.service.RtcEventPublisher
import com.shterneregen.securelan.webrtc.service.RtcSessionRequest
import com.shterneregen.securelan.webrtc.service.RtcSessionService
import com.shterneregen.securelan.webrtc.service.RtcSessionSnapshot
import com.shterneregen.securelan.webrtc.service.RtcSignalingGateway
import java.util.Objects
import java.util.Optional
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import java.util.Locale

open class DefaultRtcSessionService : RtcSessionService {
    private val eventPublisher: RtcEventPublisher
    private val signalingGateway: RtcSignalingGateway
    private val rtcExecutor: ExecutorService
    private val engineLock = Any()
    private val rtcEngine = AtomicReference(UNINITIALIZED_ENGINE)
    private val currentSession = AtomicReference<RtcSessionSnapshot?>()

    constructor(eventPublisher: RtcEventPublisher, signalingGateway: RtcSignalingGateway) {
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null")
        this.signalingGateway = Objects.requireNonNull(signalingGateway, "signalingGateway must not be null")
        rtcExecutor = createRtcExecutor()
    }

    constructor(eventPublisher: RtcEventPublisher, signalingGateway: RtcSignalingGateway, rtcEngine: RtcEngine) {
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null")
        this.signalingGateway = Objects.requireNonNull(signalingGateway, "signalingGateway must not be null")
        rtcExecutor = createRtcExecutor()
        this.rtcEngine.set(Objects.requireNonNull(rtcEngine, "rtcEngine must not be null"))
    }

    override fun runtimeStatus(): RtcRuntimeStatus = rtcEngine.get().status()

    override fun currentSession(): Optional<RtcSessionSnapshot> = Optional.ofNullable(currentSession.get())

    override fun startSession(request: RtcSessionRequest): RtcSessionSnapshot {
        validateRequest(request)

        val sessionId = UUID.randomUUID().toString()
        val snapshot = RtcSessionSnapshot(
            sessionId,
            request.localPeer(),
            request.remotePeer(),
            request.mode(),
            request.dataChannelLabel(),
            RtcSessionState.NEGOTIATING,
            buildRequestMessage(request.mode()),
        )
        currentSession.set(snapshot)
        publishState(snapshot)

        executeOnRtcThread {
            val engine = ensureRtcEngine()
            val status = engine.status()
            if (!status.available) {
                markUnavailable(sessionId, status.message)
                return@executeOnRtcThread
            }

            engine.startSession(
                sessionId,
                request.localPeer(),
                request.remotePeer(),
                request.mode(),
                request.dataChannelLabel(),
                request.audioCaptureDeviceId(),
                request.videoCaptureDeviceId(),
                Consumer(::sendSignal),
                Consumer(::forwardEvent),
            )
        }

        return snapshot
    }

    override fun acceptInboundSignal(localPeer: String?, signal: RtcSignalEnvelope?) {
        if (signal == null) {
            return
        }
        if (localPeer == null || localPeer.isBlank() || !signal.targets(localPeer)) {
            return
        }

        val snapshot = currentSession.updateAndGet { existing ->
            if (existing != null && existing.sessionId == signal.sessionId()) {
                existing
            } else {
                RtcSessionSnapshot(
                    signal.sessionId(),
                    localPeer,
                    signal.fromPeer(),
                    signal.mode(),
                    signal.dataChannelLabel(),
                    RtcSessionState.CONNECTING,
                    "Inbound ${signal.type().name.lowercase(Locale.getDefault()).replace('_', ' ')} from ${signal.fromPeer()}",
                )
            }
        } ?: return
        publishState(snapshot)

        if (signal.type() == RtcSignalType.HANGUP) {
            val closedSnapshot = withState(
                snapshot,
                RtcSessionState.CLOSED,
                if (signal.message().isBlank()) "Remote peer closed the realtime session" else signal.message(),
            )
            currentSession.set(closedSnapshot)
            publishState(closedSnapshot)

            executeOnRtcThread {
                val engine = rtcEngine.get()
                if (engine !== UNINITIALIZED_ENGINE && engine.status().available) {
                    engine.closeSession(signal.sessionId(), Consumer(::forwardEvent))
                }
            }
            return
        }

        executeOnRtcThread {
            val engine = ensureRtcEngine()
            val status = engine.status()
            if (!status.available) {
                eventPublisher.publish(RtcRuntimeWarningEvent(status.message))
                return@executeOnRtcThread
            }
            engine.handleRemoteSignal(signal, Consumer(::sendSignal), Consumer(::forwardEvent))
        }
    }

    override fun sendDataMessage(payload: String?) {
        val snapshot = currentSession.get()
        if (snapshot == null) {
            eventPublisher.publish(RtcRuntimeWarningEvent("No active realtime session."))
            return
        }

        eventPublisher.publish(RtcDataMessageEvent(snapshot.sessionId, snapshot.remotePeer, true, payload))

        executeOnRtcThread {
            val engine = ensureRtcEngine()
            val status = engine.status()
            if (!status.available) {
                eventPublisher.publish(RtcRuntimeWarningEvent(status.message))
                return@executeOnRtcThread
            }
            engine.sendData(snapshot.sessionId, payload, Consumer(::forwardEvent))
        }
    }

    override fun closeCurrentSession() {
        val snapshot = currentSession.get() ?: return

        val closingSnapshot = withState(snapshot, RtcSessionState.CLOSING, "Closing realtime session")
        currentSession.set(closingSnapshot)
        publishState(closingSnapshot)

        sendSignal(
            RtcSignalEnvelope.hangup(
                snapshot.sessionId ?: "",
                snapshot.localPeer ?: "",
                snapshot.remotePeer ?: "",
                snapshot.mode ?: RtcSessionMode.DATA,
                snapshot.dataChannelLabel ?: "",
                "Session closed by local peer",
            ),
        )

        executeOnRtcThread {
            val engine = rtcEngine.get()
            if (engine !== UNINITIALIZED_ENGINE && engine.status().available) {
                engine.closeSession(snapshot.sessionId, Consumer(::forwardEvent))
            }
        }

        val closedSnapshot = withState(snapshot, RtcSessionState.CLOSED, "Realtime session closed")
        currentSession.set(closedSnapshot)
        publishState(closedSnapshot)
    }

    override fun close() {
        closeCurrentSession()

        try {
            rtcExecutor.submit {
                val engine = rtcEngine.get()
                if (engine !== UNINITIALIZED_ENGINE) {
                    try {
                        engine.close()
                    } catch (error: Throwable) {
                        eventPublisher.publish(RtcRuntimeWarningEvent("Failed to close RTC engine: ${rootMessage(error)}"))
                    }
                }
            }
        } catch (_: RejectedExecutionException) {
            // already shutting down
        } finally {
            rtcExecutor.shutdown()
        }
    }

    private fun validateRequest(request: RtcSessionRequest) {
        Objects.requireNonNull(request, "request must not be null")
        if (request.localPeer().isBlank()) {
            throw IllegalArgumentException("Connect chat first so the local nickname is known")
        }
        if (request.remotePeer().isBlank()) {
            throw IllegalArgumentException("Remote peer nickname is required for realtime sessions")
        }
    }

    private fun sendSignal(signal: RtcSignalEnvelope) {
        signalingGateway.send(signal)
    }

    private fun forwardEvent(event: RtcEvent) {
        if (event is RtcStateChangedEvent) {
            currentSession.updateAndGet { existing ->
                if (existing == null || existing.sessionId != event.sessionId) {
                    existing
                } else {
                    RtcSessionSnapshot(
                        existing.sessionId,
                        existing.localPeer,
                        existing.remotePeer,
                        existing.mode,
                        existing.dataChannelLabel,
                        event.state,
                        event.message,
                    )
                }
            }
        }
        eventPublisher.publish(event)
    }

    private fun publishState(snapshot: RtcSessionSnapshot) {
        eventPublisher.publish(
            RtcStateChangedEvent(
                snapshot.sessionId,
                snapshot.remotePeer,
                snapshot.mode,
                snapshot.state,
                snapshot.message,
            ),
        )
    }

    private fun withState(snapshot: RtcSessionSnapshot, state: RtcSessionState, message: String?): RtcSessionSnapshot =
        RtcSessionSnapshot(
            snapshot.sessionId,
            snapshot.localPeer,
            snapshot.remotePeer,
            snapshot.mode,
            snapshot.dataChannelLabel,
            state,
            message,
        )

    private fun buildRequestMessage(mode: RtcSessionMode): String = when (mode) {
        RtcSessionMode.DATA -> "Preparing RTCDataChannel session"
        RtcSessionMode.AUDIO -> "Preparing audio call"
        RtcSessionMode.VIDEO -> "Preparing video stream"
        RtcSessionMode.AUDIO_VIDEO -> "Preparing audio/video call"
    }

    private fun createRtcExecutor(): ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "securelan-rtc").apply { isDaemon = true }
    }

    private fun executeOnRtcThread(action: Runnable) {
        try {
            rtcExecutor.submit {
                try {
                    action.run()
                } catch (error: Throwable) {
                    eventPublisher.publish(RtcRuntimeWarningEvent("RTC operation failed: ${rootMessage(error)}"))
                }
            }
        } catch (error: RejectedExecutionException) {
            eventPublisher.publish(RtcRuntimeWarningEvent("RTC executor is shutting down: ${rootMessage(error)}"))
        }
    }

    private fun ensureRtcEngine(): RtcEngine {
        var existing = rtcEngine.get()
        if (existing !== UNINITIALIZED_ENGINE) {
            return existing
        }

        synchronized(engineLock) {
            existing = rtcEngine.get()
            if (existing !== UNINITIALIZED_ENGINE) {
                return existing
            }

            val created = RtcEngineProvider.createDefault()
            rtcEngine.set(created)
            return created
        }
    }

    private fun markUnavailable(sessionId: String?, message: String?) {
        currentSession.updateAndGet { existing ->
            if (existing == null || existing.sessionId != sessionId) {
                existing
            } else {
                withState(existing, RtcSessionState.UNAVAILABLE, message)
            }
        }

        val snapshot = currentSession.get()
        if (snapshot != null && snapshot.sessionId == sessionId) {
            publishState(snapshot)
        }
        eventPublisher.publish(RtcRuntimeWarningEvent(message))
    }

    private fun rootMessage(error: Throwable): String {
        var current = error
        while (current.cause != null && current.cause !== current) {
            current = current.cause!!
        }
        return current::class.java.simpleName + (current.message?.let { ": $it" } ?: "")
    }

    private companion object {
        private val UNINITIALIZED_ENGINE: RtcEngine = NoOpRtcEngine("RTC runtime will initialize on first realtime action.")
    }
}
