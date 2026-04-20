package com.shterneregen.securelan.webrtc.service.impl;

import com.shterneregen.securelan.common.model.rtc.RtcSessionMode;
import com.shterneregen.securelan.common.model.rtc.RtcSessionState;
import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope;
import com.shterneregen.securelan.common.model.rtc.RtcSignalType;
import com.shterneregen.securelan.webrtc.event.RtcDataMessageEvent;
import com.shterneregen.securelan.webrtc.event.RtcEvent;
import com.shterneregen.securelan.webrtc.event.RtcRuntimeWarningEvent;
import com.shterneregen.securelan.webrtc.event.RtcStateChangedEvent;
import com.shterneregen.securelan.webrtc.runtime.NoOpRtcEngine;
import com.shterneregen.securelan.webrtc.runtime.RtcEngine;
import com.shterneregen.securelan.webrtc.runtime.RtcEngineProvider;
import com.shterneregen.securelan.webrtc.runtime.RtcRuntimeStatus;
import com.shterneregen.securelan.webrtc.service.RtcEventPublisher;
import com.shterneregen.securelan.webrtc.service.RtcSessionRequest;
import com.shterneregen.securelan.webrtc.service.RtcSessionService;
import com.shterneregen.securelan.webrtc.service.RtcSessionSnapshot;
import com.shterneregen.securelan.webrtc.service.RtcSignalingGateway;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;

public class DefaultRtcSessionService implements RtcSessionService {
    private static final RtcEngine UNINITIALIZED_ENGINE =
            new NoOpRtcEngine("RTC runtime will initialize on first realtime action.");

    private final RtcEventPublisher eventPublisher;
    private final RtcSignalingGateway signalingGateway;
    private final ExecutorService rtcExecutor;
    private final Object engineLock = new Object();
    private final AtomicReference<RtcEngine> rtcEngine = new AtomicReference<>(UNINITIALIZED_ENGINE);
    private final AtomicReference<RtcSessionSnapshot> currentSession = new AtomicReference<>();

    public DefaultRtcSessionService(RtcEventPublisher eventPublisher, RtcSignalingGateway signalingGateway) {
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.signalingGateway = Objects.requireNonNull(signalingGateway, "signalingGateway must not be null");
        this.rtcExecutor = createRtcExecutor();
    }

    public DefaultRtcSessionService(RtcEventPublisher eventPublisher, RtcSignalingGateway signalingGateway, RtcEngine rtcEngine) {
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.signalingGateway = Objects.requireNonNull(signalingGateway, "signalingGateway must not be null");
        this.rtcExecutor = createRtcExecutor();
        this.rtcEngine.set(Objects.requireNonNull(rtcEngine, "rtcEngine must not be null"));
    }

    @Override
    public RtcRuntimeStatus runtimeStatus() {
        return rtcEngine.get().status();
    }

    @Override
    public Optional<RtcSessionSnapshot> currentSession() {
        return Optional.ofNullable(currentSession.get());
    }

    @Override
    public RtcSessionSnapshot startSession(RtcSessionRequest request) {
        validateRequest(request);

        String sessionId = UUID.randomUUID().toString();
        RtcSessionSnapshot snapshot = new RtcSessionSnapshot(
                sessionId,
                request.localPeer(),
                request.remotePeer(),
                request.mode(),
                request.dataChannelLabel(),
                RtcSessionState.NEGOTIATING,
                buildRequestMessage(request.mode())
        );
        currentSession.set(snapshot);
        publishState(snapshot);

        executeOnRtcThread(() -> {
            RtcEngine engine = ensureRtcEngine();
            RtcRuntimeStatus status = engine.status();
            if (!status.available()) {
                markUnavailable(sessionId, status.message());
                return;
            }

            engine.startSession(
                    sessionId,
                    request.localPeer(),
                    request.remotePeer(),
                    request.mode(),
                    request.dataChannelLabel(),
                    this::sendSignal,
                    this::forwardEvent
            );
        });

        return snapshot;
    }

    @Override
    public void acceptInboundSignal(String localPeer, RtcSignalEnvelope signal) {
        if (signal == null) {
            return;
        }
        if (localPeer == null || localPeer.isBlank() || !signal.targets(localPeer)) {
            return;
        }

        RtcSessionSnapshot snapshot = currentSession.updateAndGet(existing -> {
            if (existing != null && existing.sessionId().equals(signal.sessionId())) {
                return existing;
            }
            return new RtcSessionSnapshot(
                    signal.sessionId(),
                    localPeer,
                    signal.fromPeer(),
                    signal.mode(),
                    signal.dataChannelLabel(),
                    RtcSessionState.CONNECTING,
                    "Inbound " + signal.type().name().toLowerCase().replace('_', ' ') + " from " + signal.fromPeer()
            );
        });
        publishState(snapshot);

        if (signal.type() == RtcSignalType.HANGUP) {
            RtcSessionSnapshot closedSnapshot = withState(
                    snapshot,
                    RtcSessionState.CLOSED,
                    signal.message().isBlank() ? "Remote peer closed the realtime session" : signal.message()
            );
            currentSession.set(closedSnapshot);
            publishState(closedSnapshot);

            executeOnRtcThread(() -> {
                RtcEngine engine = rtcEngine.get();
                if (engine != UNINITIALIZED_ENGINE && engine.status().available()) {
                    engine.closeSession(signal.sessionId(), this::forwardEvent);
                }
            });
            return;
        }

        executeOnRtcThread(() -> {
            RtcEngine engine = ensureRtcEngine();
            RtcRuntimeStatus status = engine.status();
            if (!status.available()) {
                eventPublisher.publish(new RtcRuntimeWarningEvent(status.message()));
                return;
            }
            engine.handleRemoteSignal(signal, this::sendSignal, this::forwardEvent);
        });
    }

    @Override
    public void sendDataMessage(String payload) {
        RtcSessionSnapshot snapshot = currentSession.get();
        if (snapshot == null) {
            eventPublisher.publish(new RtcRuntimeWarningEvent("No active realtime session."));
            return;
        }

        eventPublisher.publish(new RtcDataMessageEvent(snapshot.sessionId(), snapshot.remotePeer(), true, payload));

        executeOnRtcThread(() -> {
            RtcEngine engine = ensureRtcEngine();
            RtcRuntimeStatus status = engine.status();
            if (!status.available()) {
                eventPublisher.publish(new RtcRuntimeWarningEvent(status.message()));
                return;
            }
            engine.sendData(snapshot.sessionId(), payload, this::forwardEvent);
        });
    }

    @Override
    public void closeCurrentSession() {
        RtcSessionSnapshot snapshot = currentSession.get();
        if (snapshot == null) {
            return;
        }

        RtcSessionSnapshot closingSnapshot = withState(snapshot, RtcSessionState.CLOSING, "Closing realtime session");
        currentSession.set(closingSnapshot);
        publishState(closingSnapshot);

        sendSignal(RtcSignalEnvelope.hangup(
                snapshot.sessionId(),
                snapshot.localPeer(),
                snapshot.remotePeer(),
                snapshot.mode(),
                snapshot.dataChannelLabel(),
                "Session closed by local peer"
        ));

        executeOnRtcThread(() -> {
            RtcEngine engine = rtcEngine.get();
            if (engine != UNINITIALIZED_ENGINE && engine.status().available()) {
                engine.closeSession(snapshot.sessionId(), this::forwardEvent);
            }
        });

        RtcSessionSnapshot closedSnapshot = withState(snapshot, RtcSessionState.CLOSED, "Realtime session closed");
        currentSession.set(closedSnapshot);
        publishState(closedSnapshot);
    }

    @Override
    public void close() {
        closeCurrentSession();

        try {
            rtcExecutor.submit(() -> {
                RtcEngine engine = rtcEngine.get();
                if (engine != UNINITIALIZED_ENGINE) {
                    try {
                        engine.close();
                    } catch (Throwable error) {
                        eventPublisher.publish(new RtcRuntimeWarningEvent(
                                "Failed to close RTC engine: " + rootMessage(error)
                        ));
                    }
                }
            });
        } catch (RejectedExecutionException ignored) {
            // already shutting down
        } finally {
            rtcExecutor.shutdown();
        }
    }

    private void validateRequest(RtcSessionRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        if (request.localPeer().isBlank()) {
            throw new IllegalArgumentException("Connect chat first so the local nickname is known");
        }
        if (request.remotePeer().isBlank()) {
            throw new IllegalArgumentException("Remote peer nickname is required for realtime sessions");
        }
    }

    private void sendSignal(RtcSignalEnvelope signal) {
        signalingGateway.send(signal);
    }

    private void forwardEvent(RtcEvent event) {
        if (event instanceof RtcStateChangedEvent stateEvent) {
            currentSession.updateAndGet(existing -> {
                if (existing == null || !existing.sessionId().equals(stateEvent.sessionId())) {
                    return existing;
                }
                return new RtcSessionSnapshot(
                        existing.sessionId(),
                        existing.localPeer(),
                        existing.remotePeer(),
                        existing.mode(),
                        existing.dataChannelLabel(),
                        stateEvent.state(),
                        stateEvent.message()
                );
            });
        }
        eventPublisher.publish(event);
    }

    private void publishState(RtcSessionSnapshot snapshot) {
        eventPublisher.publish(new RtcStateChangedEvent(
                snapshot.sessionId(),
                snapshot.remotePeer(),
                snapshot.mode(),
                snapshot.state(),
                snapshot.message()
        ));
    }

    private RtcSessionSnapshot withState(RtcSessionSnapshot snapshot, RtcSessionState state, String message) {
        return new RtcSessionSnapshot(
                snapshot.sessionId(),
                snapshot.localPeer(),
                snapshot.remotePeer(),
                snapshot.mode(),
                snapshot.dataChannelLabel(),
                state,
                message
        );
    }

    private String buildRequestMessage(RtcSessionMode mode) {
        return switch (mode) {
            case DATA -> "Preparing RTCDataChannel session";
            case AUDIO -> "Preparing audio call";
            case VIDEO -> "Preparing video stream";
            case AUDIO_VIDEO -> "Preparing audio/video call";
        };
    }

    private ExecutorService createRtcExecutor() {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "securelan-rtc");
            thread.setDaemon(true);
            return thread;
        });
    }

    private void executeOnRtcThread(Runnable action) {
        try {
            rtcExecutor.submit(() -> {
                try {
                    action.run();
                } catch (Throwable error) {
                    eventPublisher.publish(new RtcRuntimeWarningEvent(
                            "RTC operation failed: " + rootMessage(error)
                    ));
                }
            });
        } catch (RejectedExecutionException error) {
            eventPublisher.publish(new RtcRuntimeWarningEvent(
                    "RTC executor is shutting down: " + rootMessage(error)
            ));
        }
    }

    private RtcEngine ensureRtcEngine() {
        RtcEngine existing = rtcEngine.get();
        if (existing != UNINITIALIZED_ENGINE) {
            return existing;
        }

        synchronized (engineLock) {
            existing = rtcEngine.get();
            if (existing != UNINITIALIZED_ENGINE) {
                return existing;
            }

            RtcEngine created = RtcEngineProvider.createDefault();
            rtcEngine.set(created);
            return created;
        }
    }

    private void markUnavailable(String sessionId, String message) {
        currentSession.updateAndGet(existing -> {
            if (existing == null || !existing.sessionId().equals(sessionId)) {
                return existing;
            }
            return withState(existing, RtcSessionState.UNAVAILABLE, message);
        });

        RtcSessionSnapshot snapshot = currentSession.get();
        if (snapshot != null && snapshot.sessionId().equals(sessionId)) {
            publishState(snapshot);
        }
        eventPublisher.publish(new RtcRuntimeWarningEvent(message));
    }

    private String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getClass().getSimpleName()
                + (current.getMessage() == null ? "" : ": " + current.getMessage());
    }
}