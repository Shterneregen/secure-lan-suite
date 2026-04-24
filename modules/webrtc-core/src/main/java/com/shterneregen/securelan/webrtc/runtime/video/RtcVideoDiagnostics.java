package com.shterneregen.securelan.webrtc.runtime.video;

import com.shterneregen.securelan.webrtc.event.RtcEvent;
import com.shterneregen.securelan.webrtc.event.RtcRuntimeWarningEvent;
import com.shterneregen.securelan.webrtc.runtime.RtcFileLogger;

import java.util.Objects;
import java.util.function.Consumer;

public final class RtcVideoDiagnostics {
    private final Consumer<RtcEvent> eventConsumer;

    public RtcVideoDiagnostics(Consumer<RtcEvent> eventConsumer) {
        this.eventConsumer = Objects.requireNonNull(eventConsumer, "eventConsumer must not be null");
    }

    public void warn(String message) {
        consoleError(message);
        RtcFileLogger.warn(message);
        eventConsumer.accept(new RtcRuntimeWarningEvent(message));
    }

    public void diag(String message) {
        consoleInfo("[diag] " + message);
        eventConsumer.accept(new RtcRuntimeWarningEvent("[diag] " + message));
    }

    public void error(String message, Throwable error) {
        consoleError(message, error);
        RtcFileLogger.error(message, error);
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
}
