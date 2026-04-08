package com.shterneregen.securelan.chat.service;

import com.shterneregen.securelan.chat.protocol.handshake.HandshakeRequest;
import com.shterneregen.securelan.chat.protocol.handshake.HandshakeResponse;
import com.shterneregen.securelan.chat.transport.ChatSocketSession;

import java.io.IOException;

public interface SecureHandshakeService {
    HandshakeResponse performClientHandshake(ChatSocketSession session, HandshakeRequest request) throws IOException;
    HandshakeResponse performServerHandshake(ChatSocketSession session, String expectedPassword, NicknameRegistryService registry) throws IOException;
}
