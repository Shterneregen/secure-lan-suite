package com.shterneregen.securelan.stego;

public final class StegoException extends RuntimeException {
    public StegoException(String message) {
        super(message);
    }

    public StegoException(String message, Throwable cause) {
        super(message, cause);
    }
}
