package com.shterneregen.securelan.chat.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

public interface SecureHandshakeService {
    String performClientHandshake(BufferedReader reader, BufferedWriter writer, String nickname, String password) throws IOException;
    HandshakeDecision performServerHandshake(BufferedReader reader, BufferedWriter writer, String expectedPassword, NicknameRegistryService registry) throws IOException;

    record HandshakeDecision(boolean accepted, String nickname, String reason) {}
}
