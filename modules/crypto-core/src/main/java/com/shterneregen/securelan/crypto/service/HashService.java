package com.shterneregen.securelan.crypto.service;

public interface HashService {
    byte[] sha256(byte[] input);

    String sha256Hex(byte[] input);
}
