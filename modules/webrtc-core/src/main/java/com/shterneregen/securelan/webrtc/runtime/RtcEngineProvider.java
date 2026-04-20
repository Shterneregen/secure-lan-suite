package com.shterneregen.securelan.webrtc.runtime;

public final class RtcEngineProvider {
    private RtcEngineProvider() {
    }

    public static RtcEngine createDefault() {
        try {
            return new WebRtcJavaEngine();
        } catch (Throwable error) {
            error.printStackTrace();
            return new NoOpRtcEngine(buildFailureMessage(error));
        }
    }

    private static String buildFailureMessage(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }

        String outer = error.getClass().getSimpleName()
                + (error.getMessage() == null ? "" : ": " + error.getMessage());
        String inner = root.getClass().getSimpleName()
                + (root.getMessage() == null ? "" : ": " + root.getMessage());

        return "Failed to initialize webrtc-java provider: "
                + outer
                + " | root cause: "
                + inner;
    }
}