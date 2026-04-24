package com.shterneregen.securelan.webrtc.runtime.video;

import com.shterneregen.securelan.common.model.rtc.RtcSessionMode;
import com.shterneregen.securelan.common.model.rtc.RtcSessionState;
import com.shterneregen.securelan.webrtc.event.RtcEvent;
import com.shterneregen.securelan.webrtc.event.RtcStateChangedEvent;
import com.shterneregen.securelan.webrtc.event.RtcVideoFrameEvent;
import dev.onvoid.webrtc.media.video.VideoFrame;
import dev.onvoid.webrtc.media.video.VideoTrackSink;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class PreviewVideoSink implements VideoTrackSink, AutoCloseable {
    private final String sessionId;
    private final String peer;
    private final RtcSessionMode mode;
    private final boolean local;
    private final Consumer<RtcEvent> eventConsumer;
    private final VideoPreviewPolicy policy;
    private final VideoFrameConverter frameConverter;
    private final RtcVideoDiagnostics diagnostics;
    private final ExecutorService previewExecutor;
    private final AtomicBoolean firstFrameLogged = new AtomicBoolean(false);
    private final AtomicBoolean previewDisabledLogged = new AtomicBoolean(false);
    private final AtomicBoolean previewConversionFailed = new AtomicBoolean(false);
    private final AtomicBoolean previewBusy = new AtomicBoolean(false);
    private final AtomicBoolean disposed = new AtomicBoolean(false);
    private final AtomicLong frameCounter = new AtomicLong(0);
    private final AtomicLong lastFramePublishedAt = new AtomicLong(0);

    public PreviewVideoSink(
            String sessionId,
            String peer,
            RtcSessionMode mode,
            boolean local,
            Consumer<RtcEvent> eventConsumer,
            VideoPreviewPolicy policy,
            VideoFrameConverter frameConverter,
            RtcVideoDiagnostics diagnostics
    ) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId must not be null");
        this.peer = Objects.requireNonNullElse(peer, "");
        this.mode = Objects.requireNonNull(mode, "mode must not be null");
        this.local = local;
        this.eventConsumer = Objects.requireNonNull(eventConsumer, "eventConsumer must not be null");
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
        this.frameConverter = Objects.requireNonNull(frameConverter, "frameConverter must not be null");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics must not be null");
        this.previewExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "rtc-preview-" + sessionId + "-" + (local ? "local" : "remote"));
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public void onVideoFrame(VideoFrame frame) {
        if (frame == null || disposed.get()) {
            return;
        }

        try {
            int width = frame.buffer.getWidth();
            int height = frame.buffer.getHeight();
            int rotation = frame.rotation;

            long frameIndex = frameCounter.incrementAndGet();
            logFrameProgress(width, height, rotation, frameIndex);

            if (!policy.enabled(local)) {
                logPreviewDisabledOnce();
                return;
            }

            if (!shouldPublishPreview()) {
                return;
            }
            if (disposed.get() || !previewBusy.compareAndSet(false, true)) {
                return;
            }

            frame.retain();
            try {
                previewExecutor.execute(() -> processFrame(frame, width, height, rotation, frameIndex));
            } catch (RejectedExecutionException error) {
                previewBusy.set(false);
                frame.release();
                logConversionFailureOnce(
                        "Failed to queue video preview work for " + streamName() + " stream of " + peer,
                        error
                );
            }
        } catch (Throwable error) {
            logConversionFailureOnce("Video preview sink failed for " + streamName() + " stream of " + peer, error);
        }
    }

    @Override
    public void close() {
        if (!disposed.compareAndSet(false, true)) {
            return;
        }
        previewExecutor.shutdown();
    }

    private void processFrame(VideoFrame frame, int width, int height, int rotation, long frameIndex) {
        try {
            if (disposed.get()) {
                return;
            }

            byte[] bgraPixels = frameConverter.convertToBgra(frame);
            if (disposed.get()) {
                return;
            }

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
            logConversionFailureOnce("Video preview conversion failed for " + streamName() + " stream of " + peer, error);
            diagnostics.diag("preview conversion failure happened after frame " + frameIndex + "; preview conversion now runs off the WebRTC callback thread to reduce crash risk");
        } finally {
            previewBusy.set(false);
            frame.release();
        }
    }

    private void logFrameProgress(int width, int height, int rotation, long frameIndex) {
        if (firstFrameLogged.compareAndSet(false, true)) {
            eventConsumer.accept(new RtcStateChangedEvent(
                    sessionId,
                    peer,
                    mode,
                    RtcSessionState.CONNECTED,
                    local ? "Sending local video frames" : "Receiving remote video frames"
            ));
            diagnostics.diag(streamName() + " video first frame " + width + "x" + height + " rotation=" + rotation + " peer=" + peer);
        } else if (frameIndex % 60 == 0) {
            diagnostics.diag(streamName() + " video frames received=" + frameIndex + " latest=" + width + "x" + height + " rotation=" + rotation + " peer=" + peer);
        }
    }

    private void logPreviewDisabledOnce() {
        if (previewDisabledLogged.compareAndSet(false, true)) {
            diagnostics.diag("video preview conversion disabled for " + streamName()
                    + " stream (set -Dsecurelan.rtc.videoPreview." + streamName() + ".enabled=true to enable)");
        }
    }

    private boolean shouldPublishPreview() {
        long now = System.nanoTime();
        long last = lastFramePublishedAt.get();
        if (last != 0 && now - last < policy.previewIntervalNanos()) {
            return false;
        }
        return lastFramePublishedAt.compareAndSet(last, now);
    }

    private void logConversionFailureOnce(String message, Throwable error) {
        if (previewConversionFailed.compareAndSet(false, true)) {
            diagnostics.warn(message + ": " + error.getClass().getSimpleName() + ": " + error.getMessage());
            diagnostics.error(message, error);
        }
    }

    private String streamName() {
        return local ? "local" : "remote";
    }
}
