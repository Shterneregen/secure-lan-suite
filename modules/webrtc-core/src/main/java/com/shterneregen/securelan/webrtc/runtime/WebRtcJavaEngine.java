package com.shterneregen.securelan.webrtc.runtime;

import com.shterneregen.securelan.common.model.rtc.RtcSessionMode;
import com.shterneregen.securelan.common.model.rtc.RtcSessionState;
import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope;
import com.shterneregen.securelan.common.model.rtc.RtcSignalType;
import com.shterneregen.securelan.webrtc.event.RtcAudioLevelEvent;
import com.shterneregen.securelan.webrtc.event.RtcDataMessageEvent;
import com.shterneregen.securelan.webrtc.event.RtcEvent;
import com.shterneregen.securelan.webrtc.event.RtcRuntimeWarningEvent;
import com.shterneregen.securelan.webrtc.event.RtcStateChangedEvent;
import com.shterneregen.securelan.webrtc.runtime.video.PreviewVideoSink;
import com.shterneregen.securelan.webrtc.runtime.video.RtcVideoDiagnostics;
import com.shterneregen.securelan.webrtc.runtime.video.VideoCapabilitySelector;
import com.shterneregen.securelan.webrtc.runtime.video.VideoCaptureSession;
import com.shterneregen.securelan.webrtc.runtime.video.VideoFrameConverter;
import com.shterneregen.securelan.webrtc.runtime.video.VideoPreviewPolicy;
import dev.onvoid.webrtc.CreateSessionDescriptionObserver;
import dev.onvoid.webrtc.PeerConnectionFactory;
import dev.onvoid.webrtc.PeerConnectionObserver;
import dev.onvoid.webrtc.RTCAnswerOptions;
import dev.onvoid.webrtc.RTCConfiguration;
import dev.onvoid.webrtc.RTCDataChannel;
import dev.onvoid.webrtc.RTCDataChannelBuffer;
import dev.onvoid.webrtc.RTCDataChannelInit;
import dev.onvoid.webrtc.RTCDataChannelObserver;
import dev.onvoid.webrtc.RTCDataChannelState;
import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCIceConnectionState;
import dev.onvoid.webrtc.RTCIceGatheringState;
import dev.onvoid.webrtc.RTCPeerConnection;
import dev.onvoid.webrtc.RTCPeerConnectionState;
import dev.onvoid.webrtc.RTCOfferOptions;
import dev.onvoid.webrtc.RTCRtpReceiver;
import dev.onvoid.webrtc.RTCRtpTransceiver;
import dev.onvoid.webrtc.RTCSessionDescription;
import dev.onvoid.webrtc.RTCSignalingState;
import dev.onvoid.webrtc.SetSessionDescriptionObserver;
import dev.onvoid.webrtc.media.MediaDevices;
import dev.onvoid.webrtc.media.MediaStream;
import dev.onvoid.webrtc.media.MediaStreamTrack;
import dev.onvoid.webrtc.media.audio.AudioDevice;
import dev.onvoid.webrtc.media.audio.AudioDeviceModule;
import dev.onvoid.webrtc.media.audio.AudioOptions;
import dev.onvoid.webrtc.media.audio.AudioTrack;
import dev.onvoid.webrtc.media.audio.AudioTrackSink;
import dev.onvoid.webrtc.media.audio.AudioTrackSource;
import dev.onvoid.webrtc.media.video.VideoTrack;
import dev.onvoid.webrtc.media.video.VideoTrackSink;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Concrete {@link RtcEngine} implementation backed by native WebRTC through the
 * {@code dev.onvoid.webrtc:webrtc-java} library.
 * <p>
 * Manages native {@link PeerConnectionFactory}, hardware {@link AudioDeviceModule},
 * peer sessions, SDP offer/answer exchanges, ICE candidate handling, RTCDataChannel
 * messaging, audio energy monitoring, and video preview capture pipelines.
 */
public final class WebRtcJavaEngine implements RtcEngine {
    private static final String PROVIDER_NAME = "webrtc-java";
    private static final String DEFAULT_DATA_CHANNEL = "securelan-data";
    private static final String STREAM_PREFIX = "securelan-stream-";
    private static final long AUDIO_LEVEL_INTERVAL_NANOS = 120_000_000L;

    private final AudioDeviceModule audioDeviceModule;
    private final PeerConnectionFactory factory;
    private final ConcurrentMap<String, PeerSession> sessions = new ConcurrentHashMap<>();
    private final RtcRuntimeStatus status;
    private final String preferredAudioCaptureDeviceId;
    private final String preferredVideoCaptureDeviceId;
    private final AtomicBoolean audioCaptureAvailable;
    private final AtomicBoolean audioPlayoutAvailable;
    private final SharedMediaDevice recording;
    private final SharedMediaDevice playout;
    private final VideoPreviewPolicy videoPreviewPolicy = VideoPreviewPolicy.fromSystemProperties();
    private final VideoFrameConverter videoFrameConverter = new VideoFrameConverter();
    private final VideoCapabilitySelector videoCapabilitySelector = new VideoCapabilitySelector();

    /**
     * Initializes the WebRTC engine using default system audio and video capture devices.
     */
    public WebRtcJavaEngine() {
        this("", "");
    }

    /**
     * Initializes the WebRTC engine using preferred audio and video hardware device identifiers.
     *
     * @param preferredAudioCaptureDeviceId descriptor of preferred microphone device, or empty for default
     * @param preferredVideoCaptureDeviceId descriptor of preferred camera device, or empty for default
     */
    public WebRtcJavaEngine(String preferredAudioCaptureDeviceId, String preferredVideoCaptureDeviceId) {
        this(preferredAudioCaptureDeviceId, preferredVideoCaptureDeviceId, "");
    }

    public WebRtcJavaEngine(String preferredAudioCaptureDeviceId, String preferredVideoCaptureDeviceId, String preferredRenderDeviceId) {
        this.preferredAudioCaptureDeviceId = normalizeDeviceId(preferredAudioCaptureDeviceId);
        this.preferredVideoCaptureDeviceId = normalizeDeviceId(preferredVideoCaptureDeviceId);
        AudioInitialization initialization = initializeAudioDeviceModule(this.preferredAudioCaptureDeviceId, preferredRenderDeviceId);
        audioDeviceModule = initialization.module();
        recording = new SharedMediaDevice(() -> { audioDeviceModule.initRecording(); audioDeviceModule.startRecording(); }, audioDeviceModule::stopRecording);
        playout = new SharedMediaDevice(() -> { audioDeviceModule.initPlayout(); audioDeviceModule.startPlayout(); }, audioDeviceModule::stopPlayout);
        audioCaptureAvailable = new AtomicBoolean(initialization.captureAvailable());
        audioPlayoutAvailable = new AtomicBoolean(initialization.playoutAvailable());
        try { factory = new PeerConnectionFactory(audioDeviceModule); }
        catch (Throwable error) {
            try { audioDeviceModule.dispose(); } catch (Throwable cleanup) { error.addSuppressed(cleanup); }
            throw error;
        }
        status = new RtcRuntimeStatus(PROVIDER_NAME, true, "Native WebRTC engine is ready");
    }

    @Override
    @NotNull
    public RtcRuntimeStatus status() {
        return status;
    }

    @Override
    public void startSession(
            String sessionId,
            String localPeer,
            String remotePeer,
            RtcSessionMode mode,
            String dataChannelLabel,
            String audioCaptureDeviceId,
            String videoCaptureDeviceId,
            @NotNull Consumer<RtcSignalEnvelope> outboundSignalConsumer,
            @NotNull Consumer<RtcEvent> eventConsumer
    ) {
        Objects.requireNonNull(outboundSignalConsumer, "outboundSignalConsumer must not be null");
        Objects.requireNonNull(eventConsumer, "eventConsumer must not be null");
        PeerSession session = createSession(sessionId, localPeer, remotePeer, mode, dataChannelLabel, audioCaptureDeviceId, videoCaptureDeviceId, outboundSignalConsumer, eventConsumer);
        session.createOffer();
    }

    @Override
    public void handleRemoteSignal(
            @NotNull RtcSignalEnvelope signal,
            @NotNull Consumer<RtcSignalEnvelope> outboundSignalConsumer,
            @NotNull Consumer<RtcEvent> eventConsumer
    ) {
        Objects.requireNonNull(signal, "signal must not be null");
        Objects.requireNonNull(outboundSignalConsumer, "outboundSignalConsumer must not be null");
        Objects.requireNonNull(eventConsumer, "eventConsumer must not be null");

        switch (signal.type()) {
            case OFFER ->
                    findOrCreateInboundSession(signal, outboundSignalConsumer, eventConsumer).applyRemoteOffer(signal);
            case ANSWER -> {
                PeerSession session = requireSession(signal.sessionId(), signal.fromPeer(), eventConsumer);
                if (session != null) {
                    session.applyRemoteAnswer(signal);
                }
            }
            case ICE_CANDIDATE -> {
                PeerSession session = requireSession(signal.sessionId(), signal.fromPeer(), eventConsumer);
                if (session != null) {
                    session.applyRemoteIceCandidate(signal);
                }
            }
            case HANGUP -> {
                PeerSession session = requireSession(signal.sessionId(), signal.fromPeer(), eventConsumer);
                if (session != null) {
                    releaseSession(signal.sessionId());
                    session.close("Remote peer closed the realtime session", true);
                }
            }
            case ERROR -> {
                PeerSession session = requireSession(signal.sessionId(), signal.fromPeer(), eventConsumer);
                if (session != null) {
                    session.fail("Remote realtime error: " + (signal.message().isBlank() ? "unknown" : signal.message()), false);
                }
            }
        }
    }

    @Override
    public void sendData(String sessionId, String payload, @NotNull Consumer<RtcEvent> eventConsumer) {
        Objects.requireNonNull(eventConsumer, "eventConsumer must not be null");
        PeerSession session = sessions.get(sessionId);
        if (session == null) {
            warn(eventConsumer, "No active RTC session with id " + sessionId);
            return;
        }
        session.sendData(payload);
    }

    @Override
    public void closeSession(String sessionId, @NotNull Consumer<RtcEvent> eventConsumer) {
        Objects.requireNonNull(eventConsumer, "eventConsumer must not be null");
        PeerSession session = sessions.remove(sessionId);
        if (session == null) {
            return;
        }
        session.close("Realtime session closed", true);
    }

    @Override
    public void close() {
        List<PeerSession> snapshots = new ArrayList<>(sessions.values());
        sessions.clear();
        for (PeerSession session : snapshots) {
            session.close("Realtime engine is shutting down", false);
        }
        try {
            factory.dispose();
        } finally {
            try { recording.close(); } finally { try { playout.close(); } finally { audioDeviceModule.dispose(); } }
        }
    }

    private PeerSession createSession(
            String sessionId,
            String localPeer,
            String remotePeer,
            RtcSessionMode mode,
            String dataChannelLabel,
            String audioCaptureDeviceId,
            String videoCaptureDeviceId,
            Consumer<RtcSignalEnvelope> outboundSignalConsumer,
            Consumer<RtcEvent> eventConsumer
    ) {
        PeerSession existing = sessions.remove(sessionId);
        if (existing != null) {
            existing.close("Replacing existing RTC session", false);
        }
        PeerSession created = new PeerSession(
                sessionId,
                localPeer,
                remotePeer,
                mode == null ? RtcSessionMode.DATA : mode,
                normalizeLabel(dataChannelLabel),
                chooseDeviceId(audioCaptureDeviceId, preferredAudioCaptureDeviceId),
                chooseDeviceId(videoCaptureDeviceId, preferredVideoCaptureDeviceId),
                true,
                outboundSignalConsumer,
                eventConsumer
        );
        sessions.put(sessionId, created);
        return created;
    }

    private PeerSession findOrCreateInboundSession(
            RtcSignalEnvelope signal,
            Consumer<RtcSignalEnvelope> outboundSignalConsumer,
            Consumer<RtcEvent> eventConsumer
    ) {
        PeerSession existing = sessions.get(signal.sessionId());
        if (existing != null && (!existing.remotePeer.equals(signal.fromPeer()) || !existing.localPeer.equals(signal.toPeer())
                || existing.mode != signal.mode() || !existing.dataChannelLabel.equals(normalizeLabel(signal.dataChannelLabel())))) {
            throw new IllegalArgumentException("RTC signal does not match the authenticated session");
        }
        return sessions.computeIfAbsent(signal.sessionId(), ignored -> new PeerSession(
                signal.sessionId(),
                signal.toPeer(),
                signal.fromPeer(),
                signal.mode(),
                normalizeLabel(signal.dataChannelLabel()),
                preferredAudioCaptureDeviceId,
                preferredVideoCaptureDeviceId,
                false,
                outboundSignalConsumer,
                eventConsumer
        ));
    }

    private PeerSession requireSession(String sessionId, String remotePeer, Consumer<RtcEvent> eventConsumer) {
        PeerSession session = sessions.get(sessionId);
        if (session != null && session.remotePeer.equals(remotePeer)) {
            return session;
        }
        warn(eventConsumer, "Ignoring realtime signal for unknown session " + sessionId + " from " + remotePeer);
        return null;
    }

    private PeerSession releaseSession(String sessionId) {
        return sessions.remove(sessionId);
    }

    private static String normalizeLabel(String value) {
        return value == null || value.isBlank() ? DEFAULT_DATA_CHANNEL : value.trim();
    }

    private static String normalizeDeviceId(String value) {
        return value == null ? "" : value.trim();
    }

    private static String chooseDeviceId(String requestedDeviceId, String fallbackDeviceId) {
        String normalized = normalizeDeviceId(requestedDeviceId);
        return normalized.isBlank() ? normalizeDeviceId(fallbackDeviceId) : normalized;
    }

    private static void consoleInfo(String message) {
        System.out.println("[rtc] " + message);
    }

    private static void consoleError(String message) {
        System.err.println("[rtc] " + message);
    }

    private static void consoleError(String message, Throwable error) {
        consoleError(message);
        if (error != null) {
            error.printStackTrace(System.err);
        }
        RtcFileLogger.error(message, error);
    }

    private static void warn(Consumer<RtcEvent> eventConsumer, String message) {
        consoleError(message);
        RtcFileLogger.warn(message);
        eventConsumer.accept(new RtcRuntimeWarningEvent(message));
    }

    private static void diag(Consumer<RtcEvent> eventConsumer, String message) {
        consoleInfo("[diag] " + message);
        eventConsumer.accept(new RtcRuntimeWarningEvent("[diag] " + message));
    }

    private static String safeToString(Object value) {
        try {
            return String.valueOf(value);
        } catch (Throwable error) {
            return value == null ? "<null>" : value.getClass().getSimpleName();
        }
    }

    private static AudioInitialization initializeAudioDeviceModule(String preferredCaptureDeviceId, String preferredRenderDeviceId) {
        AudioDeviceModule module = new AudioDeviceModule();
        boolean captureAvailable = false;
        boolean playoutAvailable = false;
        try {
            AudioDevice captureDevice = selectAudioCaptureDevice(preferredCaptureDeviceId);
            if (captureDevice != null) {
                module.setRecordingDevice(captureDevice);
                captureAvailable = true;
            }
        } catch (Throwable error) {
            consoleError("Failed during RTC audio capture device selection step", error);
        }
        try {
            AudioDevice defaultRender = normalizeDeviceId(preferredRenderDeviceId).isBlank()
                    ? MediaDevices.getDefaultAudioRenderDevice()
                    : MediaDevices.getAudioRenderDevices().stream()
                        .filter(device -> preferredRenderDeviceId.equals(device.getDescriptor())).findFirst()
                        .orElseThrow(() -> new IllegalStateException("Selected speaker is unavailable"));
            if (defaultRender != null) {
                module.setPlayoutDevice(defaultRender);
                playoutAvailable = true;
            }
        } catch (Throwable error) {
            consoleError("Failed during RTC audio playout device selection step", error);
        }
        if (captureAvailable) {
            try {
                module.initRecording();
            } catch (Throwable error) {
                captureAvailable = false;
                consoleError("Failed to initialize audio recording module", error);
            }
        }
        if (playoutAvailable) {
            try {
                module.initPlayout();
            } catch (Throwable error) {
                playoutAvailable = false;
                consoleError("Failed to initialize audio playout module", error);
            }
        }
        return new AudioInitialization(module, captureAvailable, playoutAvailable);
    }

    private static AudioDevice selectAudioCaptureDevice(String preferredCaptureDeviceId) {
        String normalized = normalizeDeviceId(preferredCaptureDeviceId);
        if (!normalized.isBlank()) {
            try {
                List<AudioDevice> devices = MediaDevices.getAudioCaptureDevices();
                if (devices != null) {
                    for (AudioDevice device : devices) {
                        if (normalized.equals(device.getDescriptor())) {
                            return device;
                        }
                    }
                }
            } catch (Throwable error) {
                throw new IllegalStateException("Selected microphone is unavailable", error);
            }
            throw new IllegalStateException("Selected microphone is unavailable");
        }
        return MediaDevices.getDefaultAudioCaptureDevice();
    }

    private AutoCloseable acquireAudioDevice(SharedMediaDevice device, AtomicBoolean available, Consumer<RtcEvent> events) {
        if (!available.get()) {
            warn(events, "Selected audio device is unavailable; choose an available device before the next call");
            return null;
        }
        try { return device.acquire(); }
        catch (Throwable error) {
            available.set(false);
            warn(events, "Audio device is unavailable");
            return null;
        }
    }

    private record AudioInitialization(AudioDeviceModule module, boolean captureAvailable, boolean playoutAvailable) {
    }

    private static final class LevelAudioSink implements AudioTrackSink {
        private final String sessionId;
        private final String peer;
        private final RtcSessionMode mode;
        private final boolean local;
        private final Consumer<RtcEvent> eventConsumer;
        private final AtomicBoolean firstFrameLogged = new AtomicBoolean(false);
        private final AtomicLong lastPublishedAt = new AtomicLong(0);

        private LevelAudioSink(String sessionId, String peer, RtcSessionMode mode, boolean local, Consumer<RtcEvent> eventConsumer) {
            this.sessionId = sessionId;
            this.peer = peer;
            this.mode = mode;
            this.local = local;
            this.eventConsumer = eventConsumer;
        }

        @Override
        public void onData(byte[] data, int bitsPerSample, int sampleRate, int channels, int frames) {
            if (firstFrameLogged.compareAndSet(false, true)) {
                eventConsumer.accept(new RtcStateChangedEvent(
                        sessionId,
                        peer,
                        mode,
                        RtcSessionState.CONNECTED,
                        local ? "Capturing local audio" : "Receiving remote audio frames"
                ));
            }

            long now = System.nanoTime();
            long last = lastPublishedAt.get();
            if (last != 0 && now - last < AUDIO_LEVEL_INTERVAL_NANOS) {
                return;
            }
            if (!lastPublishedAt.compareAndSet(last, now)) {
                return;
            }

            double level = computeLevel(data, bitsPerSample);
            eventConsumer.accept(new RtcAudioLevelEvent(sessionId, peer, local, level, level > 0.03d));
        }

        private static double computeLevel(byte[] data, int bitsPerSample) {
            if (data == null || data.length == 0 || bitsPerSample <= 0) {
                return 0d;
            }
            if (bitsPerSample != 16) {
                double energy = 0d;
                for (byte datum : data) {
                    double sample = datum / 128.0d;
                    energy += sample * sample;
                }
                return Math.min(1d, Math.sqrt(energy / data.length));
            }

            int sampleCount = data.length / 2;
            if (sampleCount == 0) {
                return 0d;
            }

            double energy = 0d;
            for (int i = 0; i < data.length - 1; i += 2) {
                int sample = (data[i] & 0xFF) | (data[i + 1] << 8);
                if (sample > 32767) {
                    sample -= 65536;
                }
                double normalized = sample / 32768.0d;
                energy += normalized * normalized;
            }
            return Math.min(1d, Math.sqrt(energy / sampleCount));
        }
    }

    private final class PeerSession implements PeerConnectionObserver {
        private final String sessionId;
        private final String localPeer;
        private final String remotePeer;
        private final RtcSessionMode mode;
        private final String dataChannelLabel;
        private final String audioCaptureDeviceId;
        private final String videoCaptureDeviceId;
        private final Consumer<RtcSignalEnvelope> outboundSignalConsumer;
        private final Consumer<RtcEvent> eventConsumer;
        private final RTCPeerConnection peerConnection;
        private final AtomicBoolean closed = new AtomicBoolean(false);
        private final AtomicBoolean remoteDescriptionReady = new AtomicBoolean(false);
        private final Queue<RtcSignalEnvelope> pendingRemoteIceCandidates = new java.util.concurrent.ArrayBlockingQueue<>(256);
        private final String streamId;

        private AutoCloseable captureLease;
        private AutoCloseable playoutLease;
        private AudioTrackSource audioSource;
        private AudioTrack audioTrack;
        private VideoCaptureSession videoCaptureSession;
        private VideoTrack videoTrack;
        private RTCDataChannel dataChannel;
        private VideoTrack remoteVideoTrack;
        private AudioTrack remoteAudioTrack;
        private VideoTrackSink localVideoSink;
        private VideoTrackSink remoteVideoSink;
        private AudioTrackSink localAudioSink;
        private AudioTrackSink remoteAudioSink;

        private PeerSession(
                String sessionId,
                String localPeer,
                String remotePeer,
                RtcSessionMode mode,
                String dataChannelLabel,
                String audioCaptureDeviceId,
                String videoCaptureDeviceId,
                boolean initiator,
                Consumer<RtcSignalEnvelope> outboundSignalConsumer,
                Consumer<RtcEvent> eventConsumer
        ) {
            this.sessionId = Objects.requireNonNull(sessionId, "sessionId must not be null");
            this.localPeer = Objects.requireNonNullElse(localPeer, "");
            this.remotePeer = Objects.requireNonNullElse(remotePeer, "");
            this.mode = Objects.requireNonNull(mode, "mode must not be null");
            this.dataChannelLabel = normalizeLabel(dataChannelLabel);
            this.audioCaptureDeviceId = normalizeDeviceId(audioCaptureDeviceId);
            this.videoCaptureDeviceId = normalizeDeviceId(videoCaptureDeviceId);
            this.outboundSignalConsumer = outboundSignalConsumer;
            this.eventConsumer = eventConsumer;
            this.streamId = STREAM_PREFIX + sessionId;
            diag(eventConsumer, "creating peer connection session=" + sessionId + " mode=" + mode + " initiator=" + initiator + " thread=" + Thread.currentThread().getName());
            this.peerConnection = factory.createPeerConnection(new RTCConfiguration(), this);
            if (this.peerConnection == null) {
                throw new IllegalStateException("PeerConnectionFactory returned null RTCPeerConnection");
            }

            try {
                attachLocalMedia();
                if (initiator) openDataChannel();
            } catch (Throwable error) {
                try { close("Peer session initialization failed", false); }
                catch (Throwable cleanup) { error.addSuppressed(cleanup); }
                throw error;
            }
        }

        private synchronized void createOffer() {
            if (closed.get()) return;
            publishState(RtcSessionState.NEGOTIATING, "Creating local offer");
            peerConnection.createOffer(new RTCOfferOptions(), new CreateSessionDescriptionObserver() {
                @Override
                public void onSuccess(RTCSessionDescription description) {
                    setLocalDescription(description, RtcSignalType.OFFER);
                }

                @Override
                public void onFailure(String error) {
                    consoleError("Failed to create local offer: " + error);
                    fail("Failed to create local offer: " + error, true);
                }
            });
        }

        private synchronized void applyRemoteOffer(RtcSignalEnvelope signal) {
            if (closed.get()) return;
            diag(eventConsumer, "applying remote offer sdpLength=" + signal.sdp().length() + " from=" + signal.fromPeer());
            publishState(RtcSessionState.CONNECTING, "Applying remote offer");
            RTCSessionDescription description = new RTCSessionDescription(dev.onvoid.webrtc.RTCSdpType.OFFER, signal.sdp());
            peerConnection.setRemoteDescription(description, new SetSessionDescriptionObserver() {
                @Override
                public void onSuccess() {
                    markRemoteDescriptionReady();
                    createAnswer();
                }

                @Override
                public void onFailure(String error) {
                    consoleError("Failed to apply remote offer: " + error);
                    fail("Failed to apply remote offer: " + error, true);
                }
            });
        }

        private synchronized void createAnswer() {
            if (closed.get()) return;
            peerConnection.createAnswer(new RTCAnswerOptions(), new CreateSessionDescriptionObserver() {
                @Override
                public void onSuccess(RTCSessionDescription description) {
                    setLocalDescription(description, RtcSignalType.ANSWER);
                }

                @Override
                public void onFailure(String error) {
                    consoleError("Failed to create answer: " + error);
                    fail("Failed to create answer: " + error, true);
                }
            });
        }

        private synchronized void applyRemoteAnswer(RtcSignalEnvelope signal) {
            if (closed.get()) return;
            diag(eventConsumer, "applying remote answer sdpLength=" + signal.sdp().length() + " from=" + signal.fromPeer());
            publishState(RtcSessionState.CONNECTING, "Applying remote answer");
            RTCSessionDescription description = new RTCSessionDescription(dev.onvoid.webrtc.RTCSdpType.ANSWER, signal.sdp());
            peerConnection.setRemoteDescription(description, new SetSessionDescriptionObserver() {
                @Override
                public void onSuccess() {
                    markRemoteDescriptionReady();
                    publishState(RtcSessionState.CONNECTING, "Remote answer applied");
                }

                @Override
                public void onFailure(String error) {
                    consoleError("Failed to apply remote answer: " + error);
                    fail("Failed to apply remote answer: " + error, true);
                }
            });
        }

        private synchronized void markRemoteDescriptionReady() {
            if (closed.get()) return;
            remoteDescriptionReady.set(true);
            flushPendingRemoteIceCandidates();
        }

        private synchronized void applyRemoteIceCandidate(RtcSignalEnvelope signal) {
            if (closed.get()) return;
            if (!remoteDescriptionReady.get()) {
                if (!pendingRemoteIceCandidates.offer(signal)) {
                    fail("Too many pending ICE candidates", true);
                    return;
                }
                diag(eventConsumer, "queued remote ICE candidate until remote description is ready session=" + sessionId + " queued=" + pendingRemoteIceCandidates.size());
                return;
            }
            addRemoteIceCandidate(signal);
        }

        private void flushPendingRemoteIceCandidates() {
            RtcSignalEnvelope pending;
            while ((pending = pendingRemoteIceCandidates.poll()) != null && !closed.get()) {
                addRemoteIceCandidate(pending);
            }
        }

        private void addRemoteIceCandidate(RtcSignalEnvelope signal) {
            try {
                diag(eventConsumer, "adding remote ICE candidate mid=" + signal.sdpMid() + " index=" + signal.sdpMLineIndex() + " candidateLength=" + signal.iceCandidate().length());
                RTCIceCandidate candidate = new RTCIceCandidate(signal.sdpMid(), signal.sdpMLineIndex(), signal.iceCandidate());
                peerConnection.addIceCandidate(candidate);
            } catch (Throwable error) {
                consoleError("Failed to add remote ICE candidate", error);
                fail("Failed to add remote ICE candidate: " + error.getMessage(), true);
            }
        }

        private synchronized void sendData(String payload) {
            if (closed.get()) return;
            if (dataChannel == null) {
                warn(eventConsumer, "RTCDataChannel is not ready yet for session " + sessionId);
                return;
            }
            if (dataChannel.getState() != RTCDataChannelState.OPEN) {
                warn(eventConsumer, "RTCDataChannel is " + dataChannel.getState() + " for session " + sessionId);
                return;
            }
            ByteBuffer buffer = ByteBuffer.wrap(payload.getBytes(StandardCharsets.UTF_8));
            try {
                dataChannel.send(new RTCDataChannelBuffer(buffer, false));
            } catch (Exception error) {
                warn(eventConsumer,
                        "Failed to send RTCDataChannel payload for session "
                                + sessionId + ": " + error.getMessage()
                );
                consoleError("Failed to send RTCDataChannel payload for session " + sessionId, error);
            }
        }

        private void close(String message, boolean emitState) {
            // Same acquisition gate as onTrack/onDataChannel/ICE. Native close runs outside it.
            synchronized (this) {
                if (!closed.compareAndSet(false, true)) return;
                pendingRemoteIceCandidates.clear();
            }
            if (emitState) {
                try { publishState(RtcSessionState.CLOSED, message); }
                catch (Throwable error) { consoleError("Closed-state publication failed", error); }
            }
            // No acquisition can pass the closed gate now. Do not hold its monitor across
            // native teardown, which can wait for an observer already entering that gate.
            if (dataChannel != null) {
                try { dataChannel.unregisterObserver(); } catch (Throwable error) { consoleError("Channel observer detach failed", error); }
                try { dataChannel.close(); } catch (Throwable error) { consoleError("Channel close failed", error); }
            }
            detachVideoSink(remoteVideoTrack, remoteVideoSink);
            detachVideoSink(videoTrack, localVideoSink);
            remoteVideoSink = null;
            localVideoSink = null;
            detachAudioSinks();
            try { peerConnection.close(); }
            catch (Throwable error) { consoleError("Peer connection close failed", error); }
            finally {
                try { disposeDataChannel(); }
                finally { try { disposeVideo(); } finally { disposeAudio(); } }
            }
        }

        private void fail(String message, boolean notifyRemote) {
            consoleError(message);
            RtcFileLogger.error(message);
            if (notifyRemote) {
                safeSendSignal(RtcSignalEnvelope.error(sessionId, localPeer, remotePeer, mode, dataChannelLabel, message));
            }
            publishState(RtcSessionState.FAILED, message);
            sessions.remove(sessionId, this);
            close(message, false);
        }

        private void attachLocalMedia() {
            if (mode.audioEnabled()) {
                attachLocalAudio();
            }
            if (mode.videoEnabled()) {
                attachLocalVideo();
            }
        }

        private void attachLocalAudio() {
            captureLease = acquireAudioDevice(recording, audioCaptureAvailable, eventConsumer);
            if (captureLease == null) {
                diag(eventConsumer, "local audio track was not added because microphone capture is unavailable for session=" + sessionId);
                return;
            }
            try {
                AudioOptions options = new AudioOptions();
                options.echoCancellation = true;
                options.autoGainControl = true;
                options.noiseSuppression = true;
                audioSource = factory.createAudioSource(options);
                audioTrack = factory.createAudioTrack("audio-" + sessionId, audioSource);
                localAudioSink = new LevelAudioSink(sessionId, localPeer, mode, true, eventConsumer);
                audioTrack.addSink(localAudioSink);
                peerConnection.addTrack(audioTrack, List.of(streamId));
                diag(eventConsumer, "local audio track added for session=" + sessionId + " streamId=" + streamId);
                if (!audioCaptureDeviceId.isBlank()) {
                    diag(eventConsumer, "preferred audio capture device selected id=" + audioCaptureDeviceId);
                }
            } catch (Throwable error) {
                disposeAudio();
                warn(eventConsumer, "Audio capture is unavailable; continuing without local microphone: " + error.getMessage());
                consoleError("Audio capture is unavailable", error);
            }
        }

        private void attachLocalVideo() {
            try {
                videoCaptureSession = VideoCaptureSession.start(
                        factory,
                        sessionId,
                        videoCaptureDeviceId,
                        new RtcVideoDiagnostics(eventConsumer),
                        videoCapabilitySelector
                );
                if (videoCaptureSession == null) {
                    return;
                }

                videoTrack = videoCaptureSession.track();
                if (videoPreviewPolicy.localPreviewEnabled()) {
                    localVideoSink = createVideoSink(localPeer, true);
                    videoTrack.addSink(localVideoSink);
                }
                peerConnection.addTrack(videoTrack, List.of(streamId));
                diag(eventConsumer, "local video track added for session=" + sessionId + " streamId=" + streamId);
            } catch (Throwable error) {
                disposeVideo();
                warn(eventConsumer, "Camera capture is unavailable: " + error.getClass().getSimpleName() + ": " + error.getMessage());
                consoleError("Camera capture is unavailable", error);
            }
        }

        private void openDataChannel() {
            try {
                RTCDataChannelInit configuration = new RTCDataChannelInit();
                configuration.ordered = true;
                configuration.maxPacketLifeTime = -1;
                configuration.maxRetransmits = -1;
                configuration.id = -1;
                configuration.negotiated = false;
                dataChannel = peerConnection.createDataChannel(dataChannelLabel, configuration);
                registerDataChannel(dataChannel);
            } catch (Throwable error) {
                warn(eventConsumer, "Unable to open RTCDataChannel: " + error.getMessage());
                consoleError("Unable to open RTCDataChannel", error);
            }
        }

        private void registerDataChannel(RTCDataChannel channel) {
            this.dataChannel = channel;
            channel.registerObserver(new RTCDataChannelObserver() {
                @Override
                public void onBufferedAmountChange(long previousAmount) {
                    // No-op, but keeping the callback registered gives us future room for file chunking / backpressure.
                }

                @Override
                public void onStateChange() {
                    if (closed.get()) return;
                    synchronized (PeerSession.this) {
                    if (closed.get()) return;
                    RTCDataChannelState state = channel.getState();
                    switch (state) {
                        case OPEN -> publishState(RtcSessionState.CONNECTED, "RTCDataChannel is open");
                        case CLOSING -> publishState(RtcSessionState.CLOSING, "RTCDataChannel is closing");
                        case CLOSED -> publishState(RtcSessionState.CLOSED, "RTCDataChannel is closed");
                        case CONNECTING -> publishState(RtcSessionState.CONNECTING, "RTCDataChannel is connecting");
                    }
                    }
                }

                @Override
                public void onMessage(RTCDataChannelBuffer buffer) {
                    if (closed.get()) return;
                    byte[] data = new byte[buffer.data.remaining()];
                    buffer.data.get(data);
                    if (buffer.binary) {
                        eventConsumer.accept(new RtcDataMessageEvent(sessionId, remotePeer, false, "[binary " + data.length + " bytes]"));
                    } else {
                        eventConsumer.accept(new RtcDataMessageEvent(sessionId, remotePeer, false, new String(data, StandardCharsets.UTF_8)));
                    }
                }
            });
        }

        private synchronized void setLocalDescription(RTCSessionDescription description, RtcSignalType signalType) {
            if (closed.get()) return;
            peerConnection.setLocalDescription(description, new SetSessionDescriptionObserver() {
                @Override
                public void onSuccess() {
                    diag(eventConsumer, "set local description success type=" + signalType + " sdpLength=" + (description.sdp == null ? 0 : description.sdp.length()));
                    if (signalType == RtcSignalType.OFFER) {
                        publishState(RtcSessionState.NEGOTIATING, "Local offer created");
                        safeSendSignal(new RtcSignalEnvelope(
                                sessionId,
                                localPeer,
                                remotePeer,
                                RtcSignalType.OFFER,
                                mode,
                                dataChannelLabel,
                                mode.audioEnabled(),
                                mode.videoEnabled(),
                                description.sdp,
                                "",
                                "",
                                -1,
                                ""
                        ));
                    } else {
                        publishState(RtcSessionState.CONNECTING, "Local answer created");
                        safeSendSignal(new RtcSignalEnvelope(
                                sessionId,
                                localPeer,
                                remotePeer,
                                RtcSignalType.ANSWER,
                                mode,
                                dataChannelLabel,
                                mode.audioEnabled(),
                                mode.videoEnabled(),
                                description.sdp,
                                "",
                                "",
                                -1,
                                ""
                        ));
                    }
                }

                @Override
                public void onFailure(String error) {
                    consoleError("Failed to set local description: " + error);
                    fail("Failed to set local description: " + error, true);
                }
            });
        }

        private void safeSendSignal(RtcSignalEnvelope signal) {
            if (closed.get()) return;
            try {
                outboundSignalConsumer.accept(signal);
            } catch (Throwable error) {
                warn(eventConsumer, "Failed to publish realtime signaling: " + error.getMessage());
                consoleError("Failed to publish realtime signaling", error);
            }
        }

        private void publishState(RtcSessionState state, String message) {
            if (closed.get() && state != RtcSessionState.CLOSED) return;
            eventConsumer.accept(new RtcStateChangedEvent(sessionId, remotePeer, mode, state, message));
        }

        private void disposeDataChannel() {
            if (dataChannel != null) {
                try {
                    dataChannel.dispose();
                } catch (Throwable cleanupError) {
                    consoleError("Failed while disposing RTC resource", cleanupError);
                }
                dataChannel = null;
            }
        }

        private void disposeVideo() {
            detachVideoSink(remoteVideoTrack, remoteVideoSink);
            detachVideoSink(videoTrack, localVideoSink);
            remoteVideoTrack = null;
            videoTrack = null;
            if (videoCaptureSession != null) {
                try {
                    videoCaptureSession.close();
                } catch (Throwable cleanupError) {
                    consoleError("Failed while disposing RTC resource", cleanupError);
                }
                videoCaptureSession = null;
            }
            localVideoSink = null;
            remoteVideoSink = null;
        }

        private void detachVideoSink(VideoTrack track, VideoTrackSink sink) {
            if (track != null && sink != null) {
                try {
                    track.removeSink(sink);
                } catch (Throwable cleanupError) {
                    consoleError("Failed while disposing RTC resource", cleanupError);
                }
            }
            if (sink instanceof PreviewVideoSink previewVideoSink) {
                previewVideoSink.close();
            }
        }

        private PreviewVideoSink createVideoSink(String peer, boolean local) {
            return new PreviewVideoSink(
                    sessionId,
                    peer,
                    mode,
                    local,
                    eventConsumer,
                    videoPreviewPolicy,
                    videoFrameConverter,
                    new RtcVideoDiagnostics(eventConsumer)
            );
        }

        private void attachRemoteVideoTrack(VideoTrack remoteTrack) {
            detachVideoSink(remoteVideoTrack, remoteVideoSink);
            remoteVideoTrack = remoteTrack;

            if (!videoPreviewPolicy.remotePreviewEnabled()) {
                remoteVideoSink = null;
                diag(eventConsumer, "remote video preview disabled for session=" + sessionId + " peer=" + remotePeer);
                publishState(RtcSessionState.CONNECTING, "Remote video track attached");
                return;
            }

            PreviewVideoSink sink = createVideoSink(remotePeer, false);
            try {
                remoteTrack.addSink(sink);
                remoteVideoSink = sink;
                diag(eventConsumer, "remote video sink attached for session=" + sessionId + " peer=" + remotePeer);
            } catch (Throwable error) {
                remoteVideoSink = null;
                sink.close();
                warn(eventConsumer, "Remote video preview is unavailable: " + error.getClass().getSimpleName() + ": " + error.getMessage());
                consoleError("Remote video preview sink attach failed", error);
            }
            publishState(RtcSessionState.CONNECTING, "Remote video track attached");
        }

        private void detachAudioSinks() {
            if (remoteAudioTrack != null && remoteAudioSink != null) {
                try {
                    remoteAudioTrack.removeSink(remoteAudioSink);
                } catch (Throwable cleanupError) {
                    consoleError("Failed while disposing RTC resource", cleanupError);
                }
            }
            if (audioTrack != null && localAudioSink != null) {
                try {
                    audioTrack.removeSink(localAudioSink);
                } catch (Throwable cleanupError) {
                    consoleError("Failed while disposing RTC resource", cleanupError);
                }
            }
            remoteAudioTrack = null;
            localAudioSink = null;
            remoteAudioSink = null;
        }

        private void disposeAudio() {
            detachAudioSinks();
            if (audioTrack != null) {
                try { audioTrack.dispose(); } catch (Throwable error) { consoleError("Audio track release failed", error); }
                audioTrack = null;
            }
            if (audioSource != null) {
                audioSource = null;
            }
            localAudioSink = null;
            remoteAudioSink = null;
            try { if (captureLease != null) captureLease.close(); } catch (Exception error) { consoleError("Unable to stop microphone capture", error); } finally { captureLease = null; }
            try { if (playoutLease != null) playoutLease.close(); } catch (Exception error) { consoleError("Unable to stop audio playout", error); } finally { playoutLease = null; }
        }

        @Override
        public void onIceCandidate(RTCIceCandidate candidate) {
            String candidateSdp = candidate.sdp == null ? "" : candidate.sdp;
            String sdpMid = candidate.sdpMid == null ? "" : candidate.sdpMid;
            diag(eventConsumer, "emitting local ICE candidate mid=" + sdpMid + " index=" + candidate.sdpMLineIndex + " candidateLength=" + candidateSdp.length());
            safeSendSignal(RtcSignalEnvelope.iceCandidate(
                    sessionId,
                    localPeer,
                    remotePeer,
                    mode,
                    dataChannelLabel,
                    candidateSdp,
                    sdpMid,
                    candidate.sdpMLineIndex
            ));
        }

        @Override
        public void onConnectionChange(RTCPeerConnectionState state) {
            diag(eventConsumer, "peer connection state=" + state + " session=" + sessionId);
            switch (state) {
                case CONNECTED -> publishState(RtcSessionState.CONNECTED, "Peer connection established");
                case CONNECTING, NEW -> publishState(RtcSessionState.CONNECTING, "Peer connection is negotiating");
                case DISCONNECTED -> publishState(RtcSessionState.CONNECTING, "Peer connection was disconnected");
                case FAILED -> fail("Peer connection failed", true);
                case CLOSED -> {
                    sessions.remove(sessionId, this);
                    close("Peer connection closed", true);
                }
            }
        }

        @Override
        public void onIceConnectionChange(RTCIceConnectionState state) {
            diag(eventConsumer, "ice connection state=" + state + " session=" + sessionId);
            switch (state) {
                case CONNECTED, COMPLETED -> publishState(RtcSessionState.CONNECTED, "ICE connected");
                case CHECKING -> publishState(RtcSessionState.CONNECTING, "Checking ICE connectivity");
                case FAILED -> fail("ICE connectivity failed", true);
                case CLOSED -> publishState(RtcSessionState.CLOSED, "ICE transport closed");
                case DISCONNECTED -> publishState(RtcSessionState.CONNECTING, "ICE was disconnected");
                default -> {
                }
            }
        }

        @Override
        public void onIceGatheringChange(RTCIceGatheringState state) {
            if (state == RTCIceGatheringState.GATHERING) {
                publishState(RtcSessionState.NEGOTIATING, "Gathering ICE candidates");
            }
        }

        @Override
        public void onSignalingChange(RTCSignalingState state) {
            if (state == RTCSignalingState.STABLE) {
                publishState(RtcSessionState.CONNECTING, "Signaling is stable");
            }
        }

        @Override
        public synchronized void onDataChannel(RTCDataChannel channel) {
            if (closed.get() || !dataChannelLabel.equals(channel.getLabel()) || dataChannel != null) { channel.close(); channel.dispose(); return; }
            registerDataChannel(channel);
        }

        @Override
        public void onRenegotiationNeeded() {
            if (!closed.get()) {
                publishState(RtcSessionState.NEGOTIATING, "Renegotiation requested by WebRTC");
            }
        }

        @Override
        public void onAddTrack(RTCRtpReceiver receiver, MediaStream[] mediaStreams) {
            MediaStreamTrack track = receiver.getTrack();
            if (track != null) {
                publishState(RtcSessionState.CONNECTING, "Remote track added: " + track.getKind());
            }
        }

        @Override
        public void onRemoveTrack(RTCRtpReceiver receiver) {
            MediaStreamTrack track = receiver.getTrack();
            if (track != null) {
                publishState(RtcSessionState.CONNECTING, "Remote track removed: " + track.getKind());
            }
        }

        @Override
        public synchronized void onTrack(RTCRtpTransceiver transceiver) {
            if (closed.get()) return;
            MediaStreamTrack track = transceiver.getReceiver().getTrack();
            if (track == null) {
                diag(eventConsumer, "received transceiver without track for session=" + sessionId);
                return;
            }

            diag(eventConsumer, "onTrack kind=" + track.getKind() + " class=" + track.getClass().getSimpleName() + " peer=" + remotePeer);

            if (MediaStreamTrack.VIDEO_TRACK_KIND.equals(track.getKind()) && track instanceof VideoTrack remoteTrack) {
                attachRemoteVideoTrack(remoteTrack);
            } else if (MediaStreamTrack.AUDIO_TRACK_KIND.equals(track.getKind()) && track instanceof AudioTrack remoteTrack) {
                remoteAudioTrack = remoteTrack;
                if (playoutLease == null) playoutLease = acquireAudioDevice(playout, audioPlayoutAvailable, eventConsumer);
                if (playoutLease != null) {
                    remoteAudioSink = new LevelAudioSink(sessionId, remotePeer, mode, false, eventConsumer);
                    remoteTrack.addSink(remoteAudioSink);
                    publishState(RtcSessionState.CONNECTING, "Remote audio track attached");
                    diag(eventConsumer, "remote audio sink attached for session=" + sessionId + " peer=" + remotePeer);
                } else {
                    remoteAudioSink = null;
                    publishState(RtcSessionState.CONNECTING, "Remote audio track attached, but local speaker output is unavailable");
                    diag(eventConsumer, "remote audio sink skipped because audio playout is unavailable for session=" + sessionId + " peer=" + remotePeer);
                }
            }
        }
    }
}
