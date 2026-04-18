package com.shterneregen.securelan.crypto.service.impl;

import com.shterneregen.securelan.crypto.service.HashService;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

public final class DefaultHashService implements HashService {
    @Override
    public byte[] sha256(byte[] input) {
        Objects.requireNonNull(input, "input");
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    @Override
    public String sha256Hex(byte[] input) {
        return HexFormat.of().formatHex(sha256(input));
    }
}
