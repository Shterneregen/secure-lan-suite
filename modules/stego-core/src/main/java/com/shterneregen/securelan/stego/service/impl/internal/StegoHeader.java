package com.shterneregen.securelan.stego.service.impl.internal;

import com.shterneregen.securelan.stego.model.StegoContentType;

import java.nio.ByteBuffer;
import java.util.Objects;

public record StegoHeader(StegoContentType contentType, boolean encrypted, int payloadLength) {
    public static final int BYTE_LENGTH = 12;

    private static final int MAGIC = 0x534C5347;
    private static final byte VERSION = 1;
    private static final byte ENCRYPTED_FLAG = 0x01;

    public StegoHeader {
        Objects.requireNonNull(contentType, "contentType");
        if (payloadLength < 0) {
            throw new IllegalArgumentException("payloadLength must not be negative");
        }
    }

    public byte[] write() {
        return ByteBuffer.allocate(BYTE_LENGTH)
                .putInt(MAGIC)
                .put(VERSION)
                .put(encrypted ? ENCRYPTED_FLAG : (byte) 0)
                .put(contentType.code())
                .put((byte) 0)
                .putInt(payloadLength)
                .array();
    }

    public static StegoHeader read(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length != BYTE_LENGTH) {
            throw new IllegalArgumentException("Invalid steganography header length");
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int magic = buffer.getInt();
        if (magic != MAGIC) {
            throw new IllegalArgumentException("No SecureLanSuite steganography payload found");
        }
        byte version = buffer.get();
        if (version != VERSION) {
            throw new IllegalArgumentException("Unsupported steganography payload version: " + version);
        }
        byte flags = buffer.get();
        boolean encrypted = (flags & ENCRYPTED_FLAG) == ENCRYPTED_FLAG;
        byte contentType = buffer.get();
        buffer.get();
        int payloadLength = buffer.getInt();
        return new StegoHeader(StegoContentType.fromCode(contentType), encrypted, payloadLength);
    }
}
