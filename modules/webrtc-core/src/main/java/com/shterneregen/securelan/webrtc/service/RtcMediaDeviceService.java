package com.shterneregen.securelan.webrtc.service;

import com.shterneregen.securelan.webrtc.event.RtcVideoFrameEvent;

import java.util.List;
import java.util.function.Consumer;

public interface RtcMediaDeviceService extends AutoCloseable {
    List<RtcMediaDevice> audioCaptureDevices();

    List<RtcMediaDevice> videoCaptureDevices();

    String testAudioCaptureDevice(String deviceId);

    String testVideoCaptureDevice(String deviceId);

    CameraPreviewSession startVideoPreview(String deviceId, Consumer<RtcVideoFrameEvent> frameConsumer);

    interface CameraPreviewSession extends AutoCloseable {
        String statusMessage();

        @Override
        void close();
    }

    @Override
    default void close() {
    }
}
