package com.shterneregen.securelan.chat.service.impl;

import com.shterneregen.securelan.chat.protocol.WireMessage;
import com.shterneregen.securelan.chat.protocol.WireMessageType;
import com.shterneregen.securelan.chat.service.NicknameRegistryService;
import com.shterneregen.securelan.chat.service.SecureHandshakeService;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

public class SimpleHandshakeService implements SecureHandshakeService {
    @Override
    public String performClientHandshake(BufferedReader reader, BufferedWriter writer, String nickname, String password) throws IOException {
        writer.write(new WireMessage(WireMessageType.HELLO, nickname, password).serialize());
        writer.newLine();
        writer.flush();

        WireMessage response = WireMessage.deserialize(reader.readLine());
        if (response.type() == WireMessageType.ACCEPTED) {
            return response.payload();
        }
        throw new IOException(response.payload().isBlank() ? "Handshake rejected" : response.payload());
    }

    @Override
    public HandshakeDecision performServerHandshake(BufferedReader reader, BufferedWriter writer, String expectedPassword, NicknameRegistryService registry) throws IOException {
        WireMessage hello = WireMessage.deserialize(reader.readLine());
        if (hello.type() != WireMessageType.HELLO) {
            writer.write(new WireMessage(WireMessageType.REJECTED, "server", "Invalid handshake").serialize());
            writer.newLine();
            writer.flush();
            return new HandshakeDecision(false, "", "Invalid handshake");
        }

        String nickname = hello.sender().trim();
        String password = hello.payload();

        if (nickname.isBlank()) {
            writer.write(new WireMessage(WireMessageType.REJECTED, "server", "Nickname must not be blank").serialize());
            writer.newLine();
            writer.flush();
            return new HandshakeDecision(false, nickname, "Nickname must not be blank");
        }
        if (!expectedPassword.equals(password)) {
            writer.write(new WireMessage(WireMessageType.REJECTED, "server", "Wrong session password").serialize());
            writer.newLine();
            writer.flush();
            return new HandshakeDecision(false, nickname, "Wrong session password");
        }
        if (!registry.register(nickname)) {
            writer.write(new WireMessage(WireMessageType.REJECTED, "server", "Nickname already in use").serialize());
            writer.newLine();
            writer.flush();
            return new HandshakeDecision(false, nickname, "Nickname already in use");
        }

        writer.write(new WireMessage(WireMessageType.ACCEPTED, "server", nickname).serialize());
        writer.newLine();
        writer.flush();
        return new HandshakeDecision(true, nickname, "");
    }
}
