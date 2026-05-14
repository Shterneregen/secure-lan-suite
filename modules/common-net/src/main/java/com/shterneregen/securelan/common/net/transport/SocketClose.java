package com.shterneregen.securelan.common.net.transport;

public final class SocketClose {
    private SocketClose() {
    }

    public static void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }
}
