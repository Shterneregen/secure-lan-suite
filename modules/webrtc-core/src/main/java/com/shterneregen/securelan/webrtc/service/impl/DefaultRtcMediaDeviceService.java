package com.shterneregen.securelan.webrtc.service.impl;

import com.shterneregen.securelan.common.model.rtc.RtcSessionMode;
import com.shterneregen.securelan.webrtc.event.RtcEvent;
import com.shterneregen.securelan.webrtc.event.RtcVideoFrameEvent;
import com.shterneregen.securelan.webrtc.runtime.RtcFileLogger;
import com.shterneregen.securelan.webrtc.runtime.video.PreviewVideoSink;
import com.shterneregen.securelan.webrtc.runtime.video.RtcVideoDiagnostics;
import com.shterneregen.securelan.webrtc.runtime.video.VideoCapabilitySelector;
import com.shterneregen.securelan.webrtc.runtime.video.VideoCaptureSession;
import com.shterneregen.securelan.webrtc.runtime.video.VideoFrameConverter;
import com.shterneregen.securelan.webrtc.runtime.video.VideoPreviewPolicy;
import com.shterneregen.securelan.webrtc.service.RtcMediaDevice;
import com.shterneregen.securelan.webrtc.service.RtcMediaDeviceService;
import dev.onvoid.webrtc.PeerConnectionFactory;
import dev.onvoid.webrtc.media.Device;
import dev.onvoid.webrtc.media.MediaDevices;
import dev.onvoid.webrtc.media.audio.AudioDevice;
import dev.onvoid.webrtc.media.video.VideoCaptureCapability;
import dev.onvoid.webrtc.media.video.VideoDevice;
import dev.onvoid.webrtc.media.video.VideoDeviceSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class DefaultRtcMediaDeviceService implements RtcMediaDeviceService {
    private static final long CAMERA_PREVIEW_INTERVAL_NANOS = 66_000_000L;

    private final AtomicReference<Thread> mediaDeviceThread = new AtomicReference<>();
    private final AtomicBoolean serviceClosed = new AtomicBoolean(false);
    private final Set<ActiveCameraPreviewSession> activePreviewSessions = ConcurrentHashMap.newKeySet();
    private final ExecutorService mediaDeviceExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "securelan-rtc-media-devices");
        thread.setDaemon(true);
        mediaDeviceThread.set(thread);
        return thread;
    });
    private final VideoFrameConverter videoFrameConverter = new VideoFrameConverter();
    private final VideoCapabilitySelector videoCapabilitySelector = new VideoCapabilitySelector();

    @Override
    public List<RtcMediaDevice> audioCaptureDevices() {
        return callOnMediaDeviceThread(
                () -> mapDevices(MediaDevices.getAudioCaptureDevices(), MediaDevices.getDefaultAudioCaptureDevice()),
                "Failed to enumerate RTC audio capture devices",
                List.of()
        );
    }

    @Override
    public List<RtcMediaDevice> videoCaptureDevices() {
        return callOnMediaDeviceThread(
                () -> mapDevices(MediaDevices.getVideoCaptureDevices(), null),
                "Failed to enumerate RTC video capture devices",
                List.of()
        );
    }

    @Override
    public String testAudioCaptureDevice(String deviceId) {
        return callOnMediaDeviceThread(() -> {
                    AudioDevice device = selectAudioDevice(deviceId);
                    if (device == null) {
                        return "Microphone test failed: no audio capture devices detected";
                    }
                    return "Microphone is available: " + name(device);
                },
                "RTC microphone test failed",
                "Microphone test failed: media device thread error"
        );
    }

    @Override
    public String testVideoCaptureDevice(String deviceId) {
        return callOnMediaDeviceThread(
                () -> testVideoCaptureDeviceOnMediaThread(deviceId),
                "RTC camera test failed",
                "Camera test failed: media device thread error"
        );
    }

    @Override
    public CameraPreviewSession startVideoPreview(String deviceId, Consumer<RtcVideoFrameEvent> frameConsumer) {
        return callOnMediaDeviceThread(
                () -> startVideoPreviewOnMediaThread(deviceId, frameConsumer),
                "RTC camera preview failed",
                new FailedCameraPreviewSession("Camera preview failed: media device thread error")
        );
    }

    @Override
    public void close() {
        if (!serviceClosed.compareAndSet(false, true)) {
            return;
        }

        for (ActiveCameraPreviewSession previewSession : List.copyOf(activePreviewSessions)) {
            previewSession.close(true);
        }

        mediaDeviceExecutor.shutdown();
        if (Thread.currentThread() == mediaDeviceThread.get()) {
            return;
        }

        try {
            if (!mediaDeviceExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                RtcFileLogger.warn("RTC media device executor did not terminate within timeout");
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            RtcFileLogger.warn("RTC media device executor shutdown interrupted");
        }
    }

    private String testVideoCaptureDeviceOnMediaThread(String deviceId) {
        VideoDeviceSource source = null;
        boolean started = false;
        try {
            VideoDevice device = selectVideoDevice(deviceId);
            if (device == null) {
                return "Camera test failed: no video capture devices detected";
            }
            source = new VideoDeviceSource();
            source.setVideoCaptureDevice(device);
            List<VideoCaptureCapability> capabilities = MediaDevices.getVideoCaptureCapabilities(device);
            VideoCaptureCapability selectedCapability = videoCapabilitySelector.select(capabilities);
            if (selectedCapability != null) {
                source.setVideoCaptureCapability(selectedCapability);
            }
            source.start();
            started = true;
            return "Camera is available: " + name(device)
                    + (selectedCapability == null ? "" : " — " + videoCapabilitySelector.describe(selectedCapability))
                    + (capabilities == null || capabilities.isEmpty() ? "" : " — " + capabilities.size() + " capabilities");
        } catch (Throwable error) {
            RtcFileLogger.warn("RTC camera test failed: " + error.getMessage());
            return "Camera test failed: " + error.getClass().getSimpleName() + ": " + error.getMessage();
        } finally {
            if (source != null) {
                if (started) {
                    try {
                        source.stop();
                    } catch (Throwable ignored) {
                    }
                }
                try {
                    source.dispose();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private CameraPreviewSession startVideoPreviewOnMediaThread(String deviceId, Consumer<RtcVideoFrameEvent> frameConsumer) {
        if (serviceClosed.get()) {
            return new FailedCameraPreviewSession("Camera preview failed: media device service is closed");
        }
        if (frameConsumer == null) {
            return new FailedCameraPreviewSession("Camera preview failed: no frame consumer configured");
        }

        PeerConnectionFactory factory = null;
        VideoCaptureSession captureSession = null;
        PreviewVideoSink sink = null;
        try {
            VideoDevice device = selectVideoDevice(deviceId);
            if (device == null) {
                return new FailedCameraPreviewSession("Camera preview failed: no video capture devices detected");
            }

            String sessionId = "camera-preview-" + UUID.randomUUID();
            List<VideoCaptureCapability> capabilities = MediaDevices.getVideoCaptureCapabilities(device);
            VideoCaptureCapability selectedCapability = videoCapabilitySelector.select(capabilities);
            factory = new PeerConnectionFactory();
            captureSession = VideoCaptureSession.start(
                    factory,
                    sessionId,
                    descriptor(device),
                    new RtcVideoDiagnostics(this::logPreviewDiagnostic),
                    videoCapabilitySelector
            );
            if (captureSession == null) {
                disposePreviewResources(captureSession, factory, sink);
                return new FailedCameraPreviewSession("Camera preview failed: no video capture devices detected");
            }
            sink = new PreviewVideoSink(
                    sessionId,
                    name(device),
                    RtcSessionMode.VIDEO,
                    true,
                    event -> forwardPreviewEvent(event, frameConsumer),
                    new VideoPreviewPolicy(true, false, CAMERA_PREVIEW_INTERVAL_NANOS),
                    videoFrameConverter,
                    new RtcVideoDiagnostics(this::logPreviewDiagnostic)
            );
            captureSession.track().addSink(sink);

            String statusMessage = "Camera preview started: " + name(device)
                    + (selectedCapability == null ? "" : " — " + videoCapabilitySelector.describe(selectedCapability))
                    + (capabilities == null || capabilities.isEmpty() ? "" : " — " + capabilities.size() + " capabilities");
            ActiveCameraPreviewSession previewSession = new ActiveCameraPreviewSession(
                    captureSession,
                    factory,
                    sink,
                    statusMessage
            );
            activePreviewSessions.add(previewSession);
            if (serviceClosed.get()) {
                previewSession.close(true);
                return new FailedCameraPreviewSession("Camera preview failed: media device service is closing");
            }
            return previewSession;
        } catch (Throwable error) {
            RtcFileLogger.warn("RTC camera preview failed: " + error.getMessage());
            disposePreviewResources(captureSession, factory, sink);
            return new FailedCameraPreviewSession("Camera preview failed: " + error.getClass().getSimpleName() + ": " + error.getMessage());
        }
    }

    private void forwardPreviewEvent(RtcEvent event, Consumer<RtcVideoFrameEvent> frameConsumer) {
        if (event instanceof RtcVideoFrameEvent frameEvent) {
            frameConsumer.accept(frameEvent);
        } else {
            logPreviewDiagnostic(event);
        }
    }

    private void logPreviewDiagnostic(RtcEvent event) {
        RtcFileLogger.warn("RTC camera preview event: " + event);
    }

    private void disposePreviewResources(VideoCaptureSession captureSession, PeerConnectionFactory factory, PreviewVideoSink sink) {
        if (captureSession != null && sink != null) {
            try {
                captureSession.track().removeSink(sink);
            } catch (Throwable ignored) {
            }
        }
        if (sink != null) {
            try {
                sink.close();
            } catch (Throwable ignored) {
            }
        }
        if (captureSession != null) {
            try {
                captureSession.close();
            } catch (Throwable ignored) {
            }
        }
        if (factory != null) {
            try {
                factory.dispose();
            } catch (Throwable ignored) {
            }
        }
    }

    private <T> T callOnMediaDeviceThread(Callable<T> action, String failureMessage, T fallbackValue) {
        if (serviceClosed.get()) {
            RtcFileLogger.warn(failureMessage + ": media device service is closed");
            return fallbackValue;
        }

        if (Thread.currentThread() == mediaDeviceThread.get()) {
            try {
                return action.call();
            } catch (Exception error) {
                return fallbackAfterFailure(failureMessage, fallbackValue, error);
            }
        }

        Future<T> future;
        try {
            future = mediaDeviceExecutor.submit(action);
        } catch (RejectedExecutionException error) {
            return fallbackAfterFailure(failureMessage, fallbackValue, error);
        }
        try {
            return future.get();
        } catch (InterruptedException error) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            RtcFileLogger.warn(failureMessage + ": interrupted");
            return fallbackValue;
        } catch (ExecutionException error) {
            Throwable cause = error.getCause() == null ? error : error.getCause();
            return fallbackAfterFailure(failureMessage, fallbackValue, cause);
        }
    }

    private <T> T fallbackAfterFailure(String failureMessage, T fallbackValue, Throwable error) {
        RtcFileLogger.warn(failureMessage + ": " + rootMessage(error));
        if (fallbackValue instanceof String) {
            @SuppressWarnings("unchecked")
            T message = (T) (failureMessage + ": " + rootMessage(error));
            return message;
        }
        return fallbackValue;
    }

    private String rootMessage(Throwable error) {
        Throwable current = error;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        if (current == null) {
            return "unknown error";
        }
        return current.getClass().getSimpleName()
                + (current.getMessage() == null ? "" : ": " + current.getMessage());
    }

    private List<RtcMediaDevice> mapDevices(List<? extends Device> devices, Device defaultDevice) {
        if (devices == null || devices.isEmpty()) {
            return List.of();
        }

        List<RtcMediaDevice> result = new ArrayList<>(devices.size());
        String defaultDescriptor = descriptor(defaultDevice);
        for (int index = 0; index < devices.size(); index++) {
            Device device = devices.get(index);
            String descriptor = descriptor(device);
            result.add(new RtcMediaDevice(
                    descriptor,
                    name(device),
                    !defaultDescriptor.isBlank() ? defaultDescriptor.equals(descriptor) : index == 0
            ));
        }
        return List.copyOf(result);
    }

    private String descriptor(Device device) {
        try {
            return device == null ? "" : device.getDescriptor();
        } catch (Throwable error) {
            return "";
        }
    }

    private String name(Device device) {
        try {
            return device == null ? "Unknown media device" : device.getName();
        } catch (Throwable error) {
            return "Unknown media device";
        }
    }

    private AudioDevice selectAudioDevice(String deviceId) {
        List<AudioDevice> devices = MediaDevices.getAudioCaptureDevices();
        if (devices == null || devices.isEmpty()) {
            return null;
        }
        String normalized = deviceId == null ? "" : deviceId.trim();
        if (!normalized.isBlank()) {
            for (AudioDevice device : devices) {
                if (normalized.equals(descriptor(device))) {
                    return device;
                }
            }
        }
        AudioDevice defaultDevice = MediaDevices.getDefaultAudioCaptureDevice();
        return defaultDevice == null ? devices.getFirst() : defaultDevice;
    }

    private VideoDevice selectVideoDevice(String deviceId) {
        List<VideoDevice> devices = MediaDevices.getVideoCaptureDevices();
        if (devices == null || devices.isEmpty()) {
            return null;
        }
        String normalized = deviceId == null ? "" : deviceId.trim();
        if (!normalized.isBlank()) {
            for (VideoDevice device : devices) {
                if (normalized.equals(descriptor(device))) {
                    return device;
                }
            }
        }
        return devices.getFirst();
    }

    private void closePreviewSession(ActiveCameraPreviewSession previewSession, boolean waitForCleanup) {
        Runnable cleanup = () -> {
            activePreviewSessions.remove(previewSession);
            disposePreviewResources(previewSession.captureSession, previewSession.factory, previewSession.sink);
        };

        if (Thread.currentThread() == mediaDeviceThread.get() || mediaDeviceExecutor.isShutdown()) {
            cleanup.run();
            return;
        }

        Future<?> future;
        try {
            future = mediaDeviceExecutor.submit(cleanup);
        } catch (RejectedExecutionException error) {
            cleanup.run();
            return;
        }

        if (!waitForCleanup) {
            return;
        }

        try {
            future.get();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            RtcFileLogger.warn("RTC camera preview close interrupted");
        } catch (ExecutionException error) {
            Throwable cause = error.getCause() == null ? error : error.getCause();
            RtcFileLogger.warn("RTC camera preview close failed: " + rootMessage(cause));
        }
    }

    private static boolean isJavaFxApplicationThread() {
        return Thread.currentThread().getName().startsWith("JavaFX");
    }

    private final class ActiveCameraPreviewSession implements CameraPreviewSession {
        private final VideoCaptureSession captureSession;
        private final PeerConnectionFactory factory;
        private final PreviewVideoSink sink;
        private final String statusMessage;
        private final AtomicBoolean closed = new AtomicBoolean(false);

        private ActiveCameraPreviewSession(
                VideoCaptureSession captureSession,
                PeerConnectionFactory factory,
                PreviewVideoSink sink,
                String statusMessage
        ) {
            this.captureSession = captureSession;
            this.factory = factory;
            this.sink = sink;
            this.statusMessage = statusMessage;
        }

        @Override
        public String statusMessage() {
            return statusMessage;
        }

        @Override
        public void close() {
            close(!isJavaFxApplicationThread());
        }

        private void close(boolean waitForCleanup) {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            closePreviewSession(this, waitForCleanup);
        }
    }

    private record FailedCameraPreviewSession(String statusMessage) implements CameraPreviewSession {
        @Override
        public void close() {
            // Nothing to close because preview startup failed before native resources were retained.
        }
    }
}
