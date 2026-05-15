package com.shterneregen.securelan.filetransfer.service.impl;

import com.shterneregen.securelan.crypto.CryptoServices;
import com.shterneregen.securelan.filetransfer.protocol.FileTransferMetadata;
import com.shterneregen.securelan.filetransfer.protocol.FileTransferSession;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Base64;

final class SecureFileTransferHandshake {
    private static final String PROTOCOL = "SECURE_FILE_TRANSFER_V1";
    private static final String ACCEPTED = "ACCEPTED";
    private static final String REJECTED_PREFIX = "REJECTED:";

    private final CryptoServices cryptoServices;

    SecureFileTransferHandshake(CryptoServices cryptoServices) {
        this.cryptoServices = cryptoServices;
    }

    FileTransferMetadata performClientHandshake(FileTransferSession session,
                                                String senderId,
                                                String recipientId,
                                                String sessionPassword,
                                                String fileName,
                                                long fileSize,
                                                String transferId) throws IOException {
        session.writeUtf(PROTOCOL);
        PublicKey serverPublicKey = cryptoServices.keyEncodingService()
                .decodePublicKey(Base64.getDecoder().decode(session.readUtf()));
        SecretKey sessionKey = cryptoServices.keyGenerationService().generateAesKey();
        FileTransferMetadata metadata = new FileTransferMetadata(transferId, senderId, recipientId, fileName, fileSize);
        String payload = sessionPassword + "\n"
                + Base64.getEncoder().encodeToString(cryptoServices.keyEncodingService().encodeSecretKey(sessionKey));
        byte[] encrypted = cryptoServices.rsaCryptoService().encrypt(payload.getBytes(StandardCharsets.UTF_8), serverPublicKey);
        session.writeBytes(encrypted);
        session.enableTransportEncryption(sessionKey, cryptoServices.aesGcmCryptoService());
        session.writeEncryptedText(metadata.compactSerialize());
        String response = session.readEncryptedText();
        if (!ACCEPTED.equals(response)) {
            throw new IOException(response);
        }
        return metadata;
    }

    void acceptTransfer(FileTransferSession session) throws IOException {
        session.writeEncryptedText(ACCEPTED);
    }

    void rejectTransfer(FileTransferSession session, String reason) throws IOException {
        session.writeEncryptedText(REJECTED_PREFIX + (reason == null || reason.isBlank() ? "Transfer rejected" : reason));
    }

    FileTransferMetadata performServerHandshake(FileTransferSession session, String expectedPassword) throws IOException {
        String protocol = session.readUtf();
        if (!PROTOCOL.equals(protocol)) {
            throw new IOException("Unsupported file transfer protocol");
        }
        KeyPair rsaKeyPair = cryptoServices.keyGenerationService().generateRsaKeyPair();
        session.writeUtf(Base64.getEncoder().encodeToString(
                cryptoServices.keyEncodingService().encodePublicKey(rsaKeyPair.getPublic())));
        byte[] encryptedPayload = session.readBytes();
        String payload = new String(
                cryptoServices.rsaCryptoService().decrypt(encryptedPayload, rsaKeyPair.getPrivate()),
                StandardCharsets.UTF_8
        );
        String[] parts = payload.split("\n", 7);
        if (parts.length != 2 && parts.length != 7) {
            throw new IOException("Malformed secure file transfer payload");
        }
        String password = parts[0];
        if (!expectedPassword.equals(password)) {
            throw new IOException("Wrong transfer password");
        }
        SecretKey sessionKey = cryptoServices.keyEncodingService().decodeAesKey(Base64.getDecoder().decode(parts[1]));
        session.enableTransportEncryption(sessionKey, cryptoServices.aesGcmCryptoService());
        return parts.length == 2 ? decodeCompactMetadata(session.readEncryptedText()) : decodeMetadata(parts);
    }

    private FileTransferMetadata decodeMetadata(String[] parts) throws IOException {
        if (parts[2].contains("|")) {
            return decodeCompactMetadata(parts[2]);
        }
        return new FileTransferMetadata(parts[2], parts[3], parts[4], parts[5], Long.parseLong(parts[6]));
    }

    private FileTransferMetadata decodeCompactMetadata(String value) throws IOException {
        try {
            return FileTransferMetadata.deserializeCompact(value);
        } catch (IllegalArgumentException exception) {
            throw new IOException("Malformed compact metadata payload", exception);
        }
    }
}
