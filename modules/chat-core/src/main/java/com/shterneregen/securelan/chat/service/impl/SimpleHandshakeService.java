package com.shterneregen.securelan.chat.service.impl;

import com.shterneregen.securelan.chat.protocol.WireMessage;
import com.shterneregen.securelan.chat.protocol.WireMessageType;
import com.shterneregen.securelan.chat.protocol.handshake.HandshakeRequest;
import com.shterneregen.securelan.chat.protocol.handshake.HandshakeResponse;
import com.shterneregen.securelan.chat.protocol.handshake.HandshakeStatus;
import com.shterneregen.securelan.chat.service.NicknameRegistryService;
import com.shterneregen.securelan.chat.service.SecureHandshakeService;
import com.shterneregen.securelan.chat.transport.ChatSocketSession;

import java.io.IOException;

public class SimpleHandshakeService implements SecureHandshakeService {
    @Override
    public HandshakeResponse performClientHandshake(ChatSocketSession session, HandshakeRequest request) throws IOException {
        session.writeMessage(new WireMessage(WireMessageType.HELLO, request.nickname(), request.sessionPassword()));
        WireMessage response = session.readMessage();
        if (response == null) {
            throw new IOException("No handshake response from server");
        }
        if (response.type() == WireMessageType.ACCEPTED) {
            return new HandshakeResponse(HandshakeStatus.ACCEPTED, response.payload(), "");
        }
        return new HandshakeResponse(HandshakeStatus.REJECTED, request.nickname(), response.payload().isBlank() ? "Handshake rejected" : response.payload());
    }

    @Override
    public HandshakeResponse performServerHandshake(ChatSocketSession session, String expectedPassword, NicknameRegistryService registry) throws IOException {
        WireMessage hello = session.readMessage();
        if (hello == null || hello.type() != WireMessageType.HELLO) {
            session.writeMessage(new WireMessage(WireMessageType.REJECTED, "server", "Invalid handshake"));
            return new HandshakeResponse(HandshakeStatus.REJECTED, "", "Invalid handshake");
        }

        String nickname = hello.sender().trim();
        String password = hello.payload();

        if (nickname.isBlank()) {
            session.writeMessage(new WireMessage(WireMessageType.REJECTED, "server", "Nickname must not be blank"));
            return new HandshakeResponse(HandshakeStatus.REJECTED, nickname, "Nickname must not be blank");
        }
        if (!expectedPassword.equals(password)) {
            session.writeMessage(new WireMessage(WireMessageType.REJECTED, "server", "Wrong session password"));
            return new HandshakeResponse(HandshakeStatus.REJECTED, nickname, "Wrong session password");
        }
        if (!registry.register(nickname)) {
            session.writeMessage(new WireMessage(WireMessageType.REJECTED, "server", "Nickname already in use"));
            return new HandshakeResponse(HandshakeStatus.REJECTED, nickname, "Nickname already in use");
        }

        session.writeMessage(new WireMessage(WireMessageType.ACCEPTED, "server", nickname));
        return new HandshakeResponse(HandshakeStatus.ACCEPTED, nickname, "");
    }
}
