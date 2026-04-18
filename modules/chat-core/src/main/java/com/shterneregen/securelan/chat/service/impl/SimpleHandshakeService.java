package com.shterneregen.securelan.chat.service.impl;

import com.shterneregen.securelan.chat.protocol.WireMessage;
import com.shterneregen.securelan.chat.protocol.WireMessageType;
import com.shterneregen.securelan.chat.protocol.handshake.HandshakeRequest;
import com.shterneregen.securelan.chat.protocol.handshake.HandshakeResponse;
import com.shterneregen.securelan.chat.protocol.handshake.HandshakeStatus;
import com.shterneregen.securelan.chat.service.NicknameRegistryService;
import com.shterneregen.securelan.chat.service.SecureHandshakeService;
import com.shterneregen.securelan.chat.transport.ChatSocketSession;
import com.shterneregen.securelan.crypto.CryptoServices;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Base64;

public class SimpleHandshakeService implements SecureHandshakeService {
    private static final String SECURE_PROTOCOL = "SECURE_V1";
    private static final String FIELD_SEPARATOR = "\n";

    private final CryptoServices cryptoServices;

    public SimpleHandshakeService() {
        this(CryptoServices.createDefault());
    }

    public SimpleHandshakeService(CryptoServices cryptoServices) {
        this.cryptoServices = cryptoServices;
    }

    @Override
    public HandshakeResponse performClientHandshake(ChatSocketSession session, HandshakeRequest request) throws IOException {
        session.writeMessage(new WireMessage(WireMessageType.HELLO, request.nickname(), SECURE_PROTOCOL));

        WireMessage serverKeyMessage = session.readMessage();
        if (serverKeyMessage == null) {
            throw new IOException("No handshake response from server");
        }
        if (serverKeyMessage.type() == WireMessageType.REJECTED) {
            return new HandshakeResponse(HandshakeStatus.REJECTED, request.nickname(), normalizeReason(serverKeyMessage.payload(), "Handshake rejected"));
        }
        if (serverKeyMessage.type() != WireMessageType.SERVER_KEY) {
            throw new IOException("Unexpected handshake response: " + serverKeyMessage.type());
        }

        PublicKey serverPublicKey = cryptoServices.keyEncodingService().decodePublicKey(Base64.getDecoder().decode(serverKeyMessage.payload()));
        SecretKey sessionKey = cryptoServices.keyGenerationService().generateAesKey();
        String clientPayload = request.nickname() + FIELD_SEPARATOR
                + request.sessionPassword() + FIELD_SEPARATOR
                + Base64.getEncoder().encodeToString(cryptoServices.keyEncodingService().encodeSecretKey(sessionKey));
        byte[] encryptedPayload = cryptoServices.rsaCryptoService().encrypt(clientPayload.getBytes(StandardCharsets.UTF_8), serverPublicKey);
        session.writeMessage(new WireMessage(WireMessageType.CLIENT_KEY, request.nickname(), Base64.getEncoder().encodeToString(encryptedPayload)));

        session.enableTransportEncryption(sessionKey, cryptoServices.aesGcmCryptoService());
        WireMessage response = session.readMessage();
        if (response == null) {
            throw new IOException("No encrypted handshake response from server");
        }
        if (response.type() == WireMessageType.ACCEPTED) {
            return new HandshakeResponse(HandshakeStatus.ACCEPTED, response.payload(), "");
        }
        return new HandshakeResponse(HandshakeStatus.REJECTED, request.nickname(), normalizeReason(response.payload(), "Handshake rejected"));
    }

    @Override
    public HandshakeResponse performServerHandshake(ChatSocketSession session, String expectedPassword, NicknameRegistryService registry) throws IOException {
        WireMessage hello = session.readMessage();
        if (hello == null || hello.type() != WireMessageType.HELLO) {
            session.writeMessage(new WireMessage(WireMessageType.REJECTED, "server", "Invalid handshake"));
            return new HandshakeResponse(HandshakeStatus.REJECTED, "", "Invalid handshake");
        }

        if (!SECURE_PROTOCOL.equals(hello.payload())) {
            session.writeMessage(new WireMessage(WireMessageType.REJECTED, "server", "Unsupported secure handshake protocol"));
            return new HandshakeResponse(HandshakeStatus.REJECTED, hello.sender(), "Unsupported secure handshake protocol");
        }

        KeyPair serverKeyPair = cryptoServices.keyGenerationService().generateRsaKeyPair();
        session.writeMessage(new WireMessage(
                WireMessageType.SERVER_KEY,
                "server",
                Base64.getEncoder().encodeToString(cryptoServices.keyEncodingService().encodePublicKey(serverKeyPair.getPublic()))
        ));

        WireMessage clientKeyMessage = session.readMessage();
        if (clientKeyMessage == null || clientKeyMessage.type() != WireMessageType.CLIENT_KEY) {
            session.writeMessage(new WireMessage(WireMessageType.REJECTED, "server", "Invalid secure key exchange"));
            return new HandshakeResponse(HandshakeStatus.REJECTED, hello.sender(), "Invalid secure key exchange");
        }

        String decryptedPayload = new String(
                cryptoServices.rsaCryptoService().decrypt(
                        Base64.getDecoder().decode(clientKeyMessage.payload()),
                        serverKeyPair.getPrivate()
                ),
                StandardCharsets.UTF_8
        );
        String[] parts = decryptedPayload.split(FIELD_SEPARATOR, 3);
        if (parts.length != 3) {
            session.writeMessage(new WireMessage(WireMessageType.REJECTED, "server", "Malformed secure handshake payload"));
            return new HandshakeResponse(HandshakeStatus.REJECTED, hello.sender(), "Malformed secure handshake payload");
        }

        String nickname = parts[0].trim();
        String password = parts[1];
        SecretKey sessionKey = cryptoServices.keyEncodingService().decodeAesKey(Base64.getDecoder().decode(parts[2]));
        session.enableTransportEncryption(sessionKey, cryptoServices.aesGcmCryptoService());

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

    private String normalizeReason(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
