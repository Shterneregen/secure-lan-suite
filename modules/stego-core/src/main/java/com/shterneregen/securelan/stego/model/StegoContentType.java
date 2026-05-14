package com.shterneregen.securelan.stego.model;

import java.util.Arrays;

public enum StegoContentType {
    BINARY((byte) 1),
    UTF8_TEXT((byte) 2);

    private final byte code;

    StegoContentType(byte code) {
        this.code = code;
    }

    public byte code() {
        return code;
    }

    public static StegoContentType fromCode(byte code) {
        return Arrays.stream(values())
                .filter(type -> type.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported steganography content type: " + code));
    }
}
