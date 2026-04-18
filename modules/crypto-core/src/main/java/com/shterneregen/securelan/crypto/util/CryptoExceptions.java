package com.shterneregen.securelan.crypto.util;

public final class CryptoExceptions {
    private CryptoExceptions() {
    }

    public static IllegalArgumentException missing(String name) {
        return new IllegalArgumentException(name + " must not be null");
    }

    public static IllegalArgumentException empty(String name) {
        return new IllegalArgumentException(name + " must not be empty");
    }

    public static IllegalStateException failed(String action, Exception cause) {
        return new IllegalStateException("Failed to " + action, cause);
    }
}
