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
import com.shterneregen.securelan.webrtc.event.RtcVideoFrameEvent;
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
import dev.onvoid.webrtc.media.FourCC;
import dev.onvoid.webrtc.media.MediaDevices;
import dev.onvoid.webrtc.media.MediaStream;
import dev.onvoid.webrtc.media.MediaStreamTrack;
import dev.onvoid.webrtc.media.audio.AudioDevice;
import dev.onvoid.webrtc.media.audio.AudioDeviceModule;
import dev.onvoid.webrtc.media.audio.AudioOptions;
import dev.onvoid.webrtc.media.audio.AudioTrack;
import dev.onvoid.webrtc.media.audio.AudioTrackSink;
import dev.onvoid.webrtc.media.audio.AudioTrackSource;
import dev.onvoid.webrtc.media.video.I420Buffer;
import dev.onvoid.webrtc.media.video.VideoCaptureCapability;
import dev.onvoid.webrtc.media.video.VideoDevice;
import dev.onvoid.webrtc.media.video.VideoDeviceSource;
import dev.onvoid.webrtc.media.video.VideoBufferConverter;
import dev.onvoid.webrtc.media.video.VideoFrame;
import dev.onvoid.webrtc.media.video.VideoFrameBuffer;
import dev.onvoid.webrtc.media.video.VideoTrack;
import dev.onvoid.webrtc.media.video.VideoTrackSink;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class WebRtcJavaEngine implements RtcEngine {
    private static final String PROVIDER_NAME = "webrtc-java";
    private static final String DEFAULT_DATA_CHANNEL = "securelan-data";
    private static final String STREAM_PREFIX = "securelan-stream-";
    private static final long VIDEO_PREVIEW_INTERVAL_NANOS = 100_000_000L;
    private static final long AUDIO_LEVEL_INTERVAL_NANOS = 120_000_000L;
    private static final boolean REMOTE_VIDEO_PREVIEW_ENABLED = Boolean.parseBoolean(System.getProperty("securelan.rtc.videoPreview.remote.enabled", "true"));
    private static final boolean LOCAL_VIDEO_PREVIEW_ENABLED = Boolean.parseBoolean(System.getProperty("securelan.rtc.videoPreview.local.enabled", "false"));

    private final AudioDeviceModule audioDeviceModule;
    private final PeerConnectionFactory factory;
    private final ConcurrentMap<String, PeerSession> sessions = new ConcurrentHashMap<>();
    private final RtcRuntimeStatus status;
    private final AtomicBoolean audioTransportStarted = new AtomicBoolean(false);

    public WebRtcJavaEngine() {
        audioDeviceModule = initializeAudioDeviceModule();
        factory = new PeerConnectionFactory(audioDeviceModule);
        status = new RtcRuntimeStatus(PROVIDER_NAME, true, "Native WebRTC engine is ready");
    }

    @Override
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
            Consumer<RtcSignalEnvelope> outboundSignalConsumer,
            Consumer<RtcEvent> eventConsumer
    ) {
        Objects.requireNonNull(outboundSignalConsumer, "outboundSignalConsumer must not be null");
        Objects.requireNonNull(eventConsumer, "eventConsumer must not be null");
        PeerSession session = createSession(sessionId, localPeer, remotePeer, mode, dataChannelLabel, true, outboundSignalConsumer, eventConsumer);
        session.createOffer();
    }

    @Override
    public void handleRemoteSignal(RtcSignalEnvelope signal, Consumer<RtcSignalEnvelope> outboundSignalConsumer, Consumer<RtcEvent> eventConsumer) {
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
                PeerSession session = releaseSession(signal.sessionId());
                if (session != null) {
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
    public void sendData(String sessionId, String payload, Consumer<RtcEvent> eventConsumer) {
        PeerSession session = sessions.get(sessionId);
        if (session == null) {
            warn(eventConsumer, "No active RTC session with id " + sessionId);
            return;
        }
        session.sendData(payload);
    }

    @Override
    public void closeSession(String sessionId, Consumer<RtcEvent> eventConsumer) {
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
            audioDeviceModule.dispose();
        }
    }

    private PeerSession createSession(
            String sessionId,
            String localPeer,
            String remotePeer,
            RtcSessionMode mode,
            String dataChannelLabel,
            boolean initiator,
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
                initiator,
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
        return sessions.computeIfAbsent(signal.sessionId(), ignored -> new PeerSession(
                signal.sessionId(),
                signal.toPeer(),
                signal.fromPeer(),
                signal.mode(),
                normalizeLabel(signal.dataChannelLabel()),
                false,
                outboundSignalConsumer,
                eventConsumer
        ));
    }

    private PeerSession requireSession(String sessionId, String remotePeer, Consumer<RtcEvent> eventConsumer) {
        PeerSession session = sessions.get(sessionId);
        if (session != null) {
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
    }

    private static void warn(Consumer<RtcEvent> eventConsumer, String message) {
        consoleError(message);
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

    private static String describeVideoCapability(VideoCaptureCapability capability) {
        if (capability == null) {
            return "<null>";
        }
        return capability.width + "x" + capability.height + "@" + capability.frameRate + "fps";
    }

    private static String summarizeCapabilities(List<VideoCaptureCapability> capabilities) {
        if (capabilities == null || capabilities.isEmpty()) {
            return "[]";
        }

        StringBuilder builder = new StringBuilder("[");
        int limit = Math.min(capabilities.size(), 8);
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(describeVideoCapability(capabilities.get(i)));
        }
        if (capabilities.size() > limit) {
            builder.append(", ... total=").append(capabilities.size());
        }
        builder.append(']');
        return builder.toString();
    }

    private static AudioDeviceModule initializeAudioDeviceModule() {
        AudioDeviceModule module = new AudioDeviceModule();
        try {
            AudioDevice defaultCapture = MediaDevices.getDefaultAudioCaptureDevice();
            if (defaultCapture != null) {
                module.setRecordingDevice(defaultCapture);
            }
        } catch (Throwable ignored) {
            consoleError("Failed during RTC audio module initialization step", ignored);
        }
        try {
            AudioDevice defaultRender = MediaDevices.getDefaultAudioRenderDevice();
            if (defaultRender != null) {
                module.setPlayoutDevice(defaultRender);
            }
        } catch (Throwable ignored) {
            consoleError("Failed during RTC audio module initialization step", ignored);
        }
        try {
            module.initRecording();
        } catch (Throwable ignored) {
            consoleError("Failed to initialize audio recording module", ignored);
        }
        try {
            module.initPlayout();
        } catch (Throwable ignored) {
            consoleError("Failed to initialize audio playout module", ignored);
        }
        return module;
    }

    private void ensureAudioTransportStarted(Consumer<RtcEvent> eventConsumer) {
        if (!audioTransportStarted.compareAndSet(false, true)) {
            return;
        }

        try {
            diag(eventConsumer, "default audio capture device: " + safeToString(MediaDevices.getDefaultAudioCaptureDevice()));
        } catch (Throwable error) {
            diag(eventConsumer, "unable to query default audio capture device: " + error.getMessage());
        }
        try {
            diag(eventConsumer, "default audio render device: " + safeToString(MediaDevices.getDefaultAudioRenderDevice()));
        } catch (Throwable error) {
            diag(eventConsumer, "unable to query default audio render device: " + error.getMessage());
        }

        boolean startedSomething = false;
        try {
            audioDeviceModule.startRecording();
            startedSomething = true;
            diag(eventConsumer, "audio recording started");
        } catch (Throwable error) {
            warn(eventConsumer, "Failed to start audio recording: " + error.getMessage());
            consoleError("Failed to start audio recording", error);
        }
        try {
            audioDeviceModule.startPlayout();
            startedSomething = true;
            diag(eventConsumer, "audio playout started");
        } catch (Throwable error) {
            warn(eventConsumer, "Failed to start audio playout: " + error.getMessage());
            consoleError("Failed to start audio playout", error);
        }

        if (!startedSomething) {
            audioTransportStarted.set(false);
            diag(eventConsumer, "audio transport failed to start any device");
        }
    }

    private static byte[] convertToBgra(VideoFrame frame) {
        I420Buffer i420 = frame.buffer.toI420();
        try {
            int width = i420.getWidth();
            int height = i420.getHeight();
            byte[] bgra = new byte[width * height * 4];

            try {
                VideoBufferConverter.convertFromI420(i420, bgra, FourCC.BGRA);
                return bgra;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to convert video frame to BGRA", e);
            }
        } finally {
            i420.release();
        }
    }

    private static final class PreviewVideoSink implements VideoTrackSink {
        private final String sessionId;
        private final String peer;
        private final RtcSessionMode mode;
        private final boolean local;
        private final Consumer<RtcEvent> eventConsumer;
        private final AtomicBoolean firstFrameLogged = new AtomicBoolean(false);
        private final AtomicBoolean previewDisabledLogged = new AtomicBoolean(false);
        private final AtomicBoolean previewConversionFailed = new AtomicBoolean(false);
        private final AtomicLong frameCounter = new AtomicLong(0);
        private final AtomicLong lastFramePublishedAt = new AtomicLong(0);

        private PreviewVideoSink(String sessionId, String peer, RtcSessionMode mode, boolean local, Consumer<RtcEvent> eventConsumer) {
            this.sessionId = sessionId;
            this.peer = peer;
            this.mode = mode;
            this.local = local;
            this.eventConsumer = eventConsumer;
        }

        @Override
        public void onVideoFrame(VideoFrame frame) {
            try {
                int width = frame.buffer.getWidth();
                int height = frame.buffer.getHeight();
                int rotation = frame.rotation;

                long frameIndex = frameCounter.incrementAndGet();
                if (firstFrameLogged.compareAndSet(false, true)) {
                    eventConsumer.accept(new RtcStateChangedEvent(
                            sessionId,
                            peer,
                            mode,
                            RtcSessionState.CONNECTED,
                            local ? "Sending local video frames" : "Receiving remote video frames"
                    ));
                    diag(eventConsumer, (local ? "local" : "remote") + " video first frame " + width + "x" + height + " rotation=" + rotation + " peer=" + peer);
                } else if (frameIndex % 60 == 0) {
                    diag(eventConsumer, (local ? "local" : "remote") + " video frames received=" + frameIndex + " latest=" + width + "x" + height + " rotation=" + rotation + " peer=" + peer);
                }

                boolean previewEnabled = local ? LOCAL_VIDEO_PREVIEW_ENABLED : REMOTE_VIDEO_PREVIEW_ENABLED;
                if (!previewEnabled) {
                    if (previewDisabledLogged.compareAndSet(false, true)) {
                        diag(eventConsumer, "video preview conversion disabled for " + (local ? "local" : "remote")
                                + " stream (set -Dsecurelan.rtc.videoPreview." + (local ? "local" : "remote") + ".enabled=true to enable)");
                    }
                    return;
                }

                long now = System.nanoTime();
                long last = lastFramePublishedAt.get();
                if (last != 0 && now - last < VIDEO_PREVIEW_INTERVAL_NANOS) {
                    return;
                }
                if (!lastFramePublishedAt.compareAndSet(last, now)) {
                    return;
                }

                try {
                    byte[] bgraPixels = convertToBgra(frame);
                    eventConsumer.accept(new RtcVideoFrameEvent(
                            sessionId,
                            peer,
                            local,
                            width,
                            height,
                            rotation,
                            bgraPixels
                    ));
                } catch (Throwable error) {
                    if (previewConversionFailed.compareAndSet(false, true)) {
                        warn(eventConsumer,
                                "Video preview conversion failed for " + (local ? "local" : "remote") + " stream of " + peer + ": " + error.getClass().getSimpleName() + ": " + error.getMessage()
                        );
                        consoleError("Video preview conversion failed for " + (local ? "local" : "remote") + " stream of " + peer, error);
                        diag(eventConsumer, "preview conversion failure happened after frame " + frameIndex + "; try toggling -Dsecurelan.rtc.videoPreview.local.enabled=false or -Dsecurelan.rtc.videoPreview.remote.enabled=false to isolate preview rendering from transport");
                    }
                }
            } finally {
                frame.release();
            }
        }
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
        private final boolean initiator;
        private final Consumer<RtcSignalEnvelope> outboundSignalConsumer;
        private final Consumer<RtcEvent> eventConsumer;
        private final RTCPeerConnection peerConnection;
        private final AtomicBoolean closed = new AtomicBoolean(false);
        private final String streamId;

        private AudioTrackSource audioSource;
        private AudioTrack audioTrack;
        private VideoDeviceSource videoSource;
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
                boolean initiator,
                Consumer<RtcSignalEnvelope> outboundSignalConsumer,
                Consumer<RtcEvent> eventConsumer
        ) {
            this.sessionId = Objects.requireNonNull(sessionId, "sessionId must not be null");
            this.localPeer = Objects.requireNonNullElse(localPeer, "");
            this.remotePeer = Objects.requireNonNullElse(remotePeer, "");
            this.mode = Objects.requireNonNull(mode, "mode must not be null");
            this.dataChannelLabel = normalizeLabel(dataChannelLabel);
            this.initiator = initiator;
            this.outboundSignalConsumer = outboundSignalConsumer;
            this.eventConsumer = eventConsumer;
            this.streamId = STREAM_PREFIX + sessionId;
            diag(eventConsumer, "creating peer connection session=" + sessionId + " mode=" + mode + " initiator=" + initiator + " thread=" + Thread.currentThread().getName());
            this.peerConnection = factory.createPeerConnection(new RTCConfiguration(), this);
            if (this.peerConnection == null) {
                throw new IllegalStateException("PeerConnectionFactory returned null RTCPeerConnection");
            }

            attachLocalMedia();
            if (initiator) {
                openDataChannel();
            }
        }

        private void createOffer() {
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

        private void applyRemoteOffer(RtcSignalEnvelope signal) {
            diag(eventConsumer, "applying remote offer sdpLength=" + (signal.sdp() == null ? 0 : signal.sdp().length()) + " from=" + signal.fromPeer());
            publishState(RtcSessionState.CONNECTING, "Applying remote offer");
            RTCSessionDescription description = new RTCSessionDescription(dev.onvoid.webrtc.RTCSdpType.OFFER, signal.sdp());
            peerConnection.setRemoteDescription(description, new SetSessionDescriptionObserver() {
                @Override
                public void onSuccess() {
                    createAnswer();
                }

                @Override
                public void onFailure(String error) {
                    consoleError("Failed to apply remote offer: " + error);
                    fail("Failed to apply remote offer: " + error, true);
                }
            });
        }

        private void createAnswer() {
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

        private void applyRemoteAnswer(RtcSignalEnvelope signal) {
            diag(eventConsumer, "applying remote answer sdpLength=" + (signal.sdp() == null ? 0 : signal.sdp().length()) + " from=" + signal.fromPeer());
            publishState(RtcSessionState.CONNECTING, "Applying remote answer");
            RTCSessionDescription description = new RTCSessionDescription(dev.onvoid.webrtc.RTCSdpType.ANSWER, signal.sdp());
            peerConnection.setRemoteDescription(description, new SetSessionDescriptionObserver() {
                @Override
                public void onSuccess() {
                    publishState(RtcSessionState.CONNECTING, "Remote answer applied");
                }

                @Override
                public void onFailure(String error) {
                    consoleError("Failed to apply remote answer: " + error);
                    fail("Failed to apply remote answer: " + error, true);
                }
            });
        }

        private void applyRemoteIceCandidate(RtcSignalEnvelope signal) {
            try {
                diag(eventConsumer, "adding remote ICE candidate mid=" + signal.sdpMid() + " index=" + signal.sdpMLineIndex() + " candidateLength=" + (signal.iceCandidate() == null ? 0 : signal.iceCandidate().length()));
                RTCIceCandidate candidate = new RTCIceCandidate(signal.sdpMid(), signal.sdpMLineIndex(), signal.iceCandidate());
                peerConnection.addIceCandidate(candidate);
            } catch (Throwable error) {
                consoleError("Failed to add remote ICE candidate", error);
                fail("Failed to add remote ICE candidate: " + error.getMessage(), true);
            }
        }

        private void sendData(String payload) {
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
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            if (emitState) {
                publishState(RtcSessionState.CLOSED, message);
            }
            disposeDataChannel();
            disposeVideo();
            disposeAudio();
            try {
                peerConnection.close();
            } catch (Throwable ignored) {
                consoleError("Failed to close peer connection for session " + sessionId, ignored);
            }
        }

        private void fail(String message, boolean notifyRemote) {
            consoleError(message);
            if (notifyRemote) {
                safeSendSignal(RtcSignalEnvelope.error(sessionId, localPeer, remotePeer, mode, dataChannelLabel, message));
            }
            publishState(RtcSessionState.FAILED, message);
            sessions.remove(sessionId, this);
            close(message, false);
        }

        private void attachLocalMedia() {
            if (mode.audioEnabled()) {
                ensureAudioTransportStarted(eventConsumer);
                attachLocalAudio();
            }
            if (mode.videoEnabled()) {
                attachLocalVideo();
            }
        }

        private void attachLocalAudio() {
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
            } catch (Throwable error) {
                warn(eventConsumer, "Audio capture is unavailable: " + error.getMessage());
                consoleError("Audio capture is unavailable", error);
            }
        }

        private void attachLocalVideo() {
            try {
                List<VideoDevice> cameras = MediaDevices.getVideoCaptureDevices();
                if (cameras == null || cameras.isEmpty()) {
                    warn(eventConsumer, "No camera devices were detected for realtime video.");
                    return;
                }

                diag(eventConsumer, "detected video devices count=" + cameras.size() + " first=" + safeToString(cameras.getFirst()));

                VideoDevice camera = cameras.getFirst();
                videoSource = new VideoDeviceSource();
                videoSource.setVideoCaptureDevice(camera);

                List<VideoCaptureCapability> capabilities = MediaDevices.getVideoCaptureCapabilities(camera);
                diag(eventConsumer, "video capabilities for " + safeToString(camera) + " -> " + summarizeCapabilities(capabilities));

                if (capabilities != null && !capabilities.isEmpty()) {
                    VideoCaptureCapability selectedCapability = selectVideoCapability(capabilities);
                    videoSource.setVideoCaptureCapability(selectedCapability);
                    diag(eventConsumer, "selected video capability " + describeVideoCapability(selectedCapability) + " for " + safeToString(camera));
                } else {
                    diag(eventConsumer, "camera reported no explicit capabilities, using default capture settings for " + safeToString(camera));
                }

                videoSource.start();
                diag(eventConsumer, "video source started for " + safeToString(camera));

                videoTrack = factory.createVideoTrack("video-" + sessionId, videoSource);
                if (LOCAL_VIDEO_PREVIEW_ENABLED) {
                    localVideoSink = new PreviewVideoSink(sessionId, localPeer, mode, true, eventConsumer);
                    videoTrack.addSink(localVideoSink);
                }
                peerConnection.addTrack(videoTrack, List.of(streamId));
                diag(eventConsumer, "local video track added for session=" + sessionId + " streamId=" + streamId);
            } catch (Throwable error) {
                warn(eventConsumer, "Camera capture is unavailable: " + error.getClass().getSimpleName() + ": " + error.getMessage());
                consoleError("Camera capture is unavailable", error);
            }
        }

        private VideoCaptureCapability selectVideoCapability(List<VideoCaptureCapability> capabilities) {
            VideoCaptureCapability best480 = null;
            VideoCaptureCapability best720 = null;
            VideoCaptureCapability fallback = capabilities.getFirst();

            for (VideoCaptureCapability capability : capabilities) {
                if (capability == null) {
                    continue;
                }

                int width = Math.max(capability.width, 0);
                int height = Math.max(capability.height, 0);
                int fps = Math.max(capability.frameRate, 0);
                int area = width * height;

                if (width <= 640 && height <= 480 && fps <= 30) {
                    if (best480 == null || area > best480.width * best480.height || (area == best480.width * best480.height && fps > best480.frameRate)) {
                        best480 = capability;
                    }
                    continue;
                }

                if (width <= 1280 && height <= 720 && fps <= 30) {
                    if (best720 == null || area > best720.width * best720.height || (area == best720.width * best720.height && fps > best720.frameRate)) {
                        best720 = capability;
                    }
                }
            }

            if (best480 != null) {
                return best480;
            }
            if (best720 != null) {
                return best720;
            }
            return fallback;
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
                    RTCDataChannelState state = channel.getState();
                    switch (state) {
                        case OPEN -> publishState(RtcSessionState.CONNECTED, "RTCDataChannel is open");
                        case CLOSING -> publishState(RtcSessionState.CLOSING, "RTCDataChannel is closing");
                        case CLOSED -> publishState(RtcSessionState.CLOSED, "RTCDataChannel is closed");
                        case CONNECTING -> publishState(RtcSessionState.CONNECTING, "RTCDataChannel is connecting");
                    }
                }

                @Override
                public void onMessage(RTCDataChannelBuffer buffer) {
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

        private void setLocalDescription(RTCSessionDescription description, RtcSignalType signalType) {
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
            try {
                outboundSignalConsumer.accept(signal);
            } catch (Throwable error) {
                warn(eventConsumer, "Failed to publish realtime signaling: " + error.getMessage());
                consoleError("Failed to publish realtime signaling", error);
            }
        }

        private void publishState(RtcSessionState state, String message) {
            eventConsumer.accept(new RtcStateChangedEvent(sessionId, remotePeer, mode, state, message));
        }

        private void disposeDataChannel() {
            if (dataChannel != null) {
                try {
                    dataChannel.close();
                } catch (Throwable ignored) {
                    consoleError("Failed while disposing RTC resource", ignored);
                }
                dataChannel = null;
            }
        }

        private void disposeVideo() {
            if (remoteVideoTrack != null && remoteVideoSink != null) {
                try {
                    remoteVideoTrack.removeSink(remoteVideoSink);
                } catch (Throwable ignored) {
                    consoleError("Failed while disposing RTC resource", ignored);
                }
            }
            if (videoTrack != null && localVideoSink != null) {
                try {
                    videoTrack.removeSink(localVideoSink);
                } catch (Throwable ignored) {
                    consoleError("Failed while disposing RTC resource", ignored);
                }
            }
            remoteVideoTrack = null;
            if (videoTrack != null) {
                videoTrack = null;
            }
            if (videoSource != null) {
                try {
                    videoSource.stop();
                } catch (Throwable ignored) {
                    consoleError("Failed while disposing RTC resource", ignored);
                }
                try {
                    videoSource.dispose();
                } catch (Throwable ignored) {
                    consoleError("Failed while disposing RTC resource", ignored);
                }
                videoSource = null;
            }
            localVideoSink = null;
            remoteVideoSink = null;
        }

        private void disposeAudio() {
            if (remoteAudioTrack != null && remoteAudioSink != null) {
                try {
                    remoteAudioTrack.removeSink(remoteAudioSink);
                } catch (Throwable ignored) {
                    consoleError("Failed while disposing RTC resource", ignored);
                }
            }
            if (audioTrack != null && localAudioSink != null) {
                try {
                    audioTrack.removeSink(localAudioSink);
                } catch (Throwable ignored) {
                    consoleError("Failed while disposing RTC resource", ignored);
                }
            }
            remoteAudioTrack = null;
            if (audioTrack != null) {
                audioTrack = null;
            }
            if (audioSource != null) {
                audioSource = null;
            }
            localAudioSink = null;
            remoteAudioSink = null;
        }

        @Override
        public void onIceCandidate(RTCIceCandidate candidate) {
            diag(eventConsumer, "emitting local ICE candidate mid=" + candidate.sdpMid + " index=" + candidate.sdpMLineIndex + " candidateLength=" + (candidate.sdp == null ? 0 : candidate.sdp.length()));
            safeSendSignal(RtcSignalEnvelope.iceCandidate(
                    sessionId,
                    localPeer,
                    remotePeer,
                    mode,
                    dataChannelLabel,
                    candidate.sdp,
                    candidate.sdpMid,
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
                    publishState(RtcSessionState.CLOSED, "Peer connection closed");
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
        public void onDataChannel(RTCDataChannel channel) {
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
        public void onTrack(RTCRtpTransceiver transceiver) {
            MediaStreamTrack track = transceiver.getReceiver().getTrack();
            if (track == null) {
                diag(eventConsumer, "received transceiver without track for session=" + sessionId);
                return;
            }

            diag(eventConsumer, "onTrack kind=" + track.getKind() + " class=" + track.getClass().getSimpleName() + " peer=" + remotePeer);

            if (MediaStreamTrack.VIDEO_TRACK_KIND.equals(track.getKind()) && track instanceof VideoTrack remoteTrack) {
                remoteVideoTrack = remoteTrack;
                if (REMOTE_VIDEO_PREVIEW_ENABLED) {
                    remoteVideoSink = new PreviewVideoSink(sessionId, remotePeer, mode, false, eventConsumer);
                    remoteTrack.addSink(remoteVideoSink);
                }
                publishState(RtcSessionState.CONNECTING, "Remote video track attached");
                diag(eventConsumer, "remote video sink attached for session=" + sessionId + " peer=" + remotePeer);
            } else if (MediaStreamTrack.AUDIO_TRACK_KIND.equals(track.getKind()) && track instanceof AudioTrack remoteTrack) {
                ensureAudioTransportStarted(eventConsumer);
                remoteAudioTrack = remoteTrack;
                remoteAudioSink = new LevelAudioSink(sessionId, remotePeer, mode, false, eventConsumer);
                remoteTrack.addSink(remoteAudioSink);
                publishState(RtcSessionState.CONNECTING, "Remote audio track attached");
                diag(eventConsumer, "remote audio sink attached for session=" + sessionId + " peer=" + remotePeer);
            }
        }
    }
}
