package com.shterneregen.securelan.stego.service.impl;

import com.shterneregen.securelan.crypto.model.PasswordEncryptedData;
import com.shterneregen.securelan.crypto.workflow.PasswordFileCryptoWorkflow;
import com.shterneregen.securelan.stego.model.BmpCapacity;
import com.shterneregen.securelan.stego.model.ExtractedStegoPayload;
import com.shterneregen.securelan.stego.model.StegoContentType;
import com.shterneregen.securelan.stego.service.SteganographyService;
import com.shterneregen.securelan.stego.service.impl.internal.BmpImage;
import com.shterneregen.securelan.stego.service.impl.internal.StegoHeader;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class BmpSteganographyService implements SteganographyService {
    private final PasswordFileCryptoWorkflow passwordFileCryptoWorkflow;

    public BmpSteganographyService(PasswordFileCryptoWorkflow passwordFileCryptoWorkflow) {
        this.passwordFileCryptoWorkflow = Objects.requireNonNull(passwordFileCryptoWorkflow, "passwordFileCryptoWorkflow");
    }

    @Override
    public BmpCapacity inspect(byte[] bmpBytes) {
        BmpImage image = BmpImage.parse(bmpBytes);
        return new BmpCapacity(
                image.width(),
                image.height(),
                image.bitsPerPixel(),
                image.carrierByteCount(),
                StegoHeader.BYTE_LENGTH,
                image.maxPayloadBytes(StegoHeader.BYTE_LENGTH)
        );
    }

    @Override
    public byte[] hide(byte[] bmpBytes, byte[] payload, StegoContentType contentType) {
        Objects.requireNonNull(contentType, "contentType");
        return hideInternal(bmpBytes, payload, contentType, false);
    }

    @Override
    public ExtractedStegoPayload extract(byte[] bmpBytes) {
        BmpImage image = BmpImage.parse(bmpBytes);
        byte[] headerBytes = image.readLeastSignificantBits(bmpBytes, StegoHeader.BYTE_LENGTH);
        StegoHeader header = StegoHeader.read(headerBytes);
        int maxPayloadBytes = image.maxPayloadBytes(StegoHeader.BYTE_LENGTH);
        if (header.payloadLength() > maxPayloadBytes) {
            throw new IllegalArgumentException("Hidden payload length exceeds BMP capacity");
        }
        byte[] hiddenBytes = image.readLeastSignificantBits(bmpBytes, StegoHeader.BYTE_LENGTH + header.payloadLength());
        byte[] payload = new byte[header.payloadLength()];
        System.arraycopy(hiddenBytes, StegoHeader.BYTE_LENGTH, payload, 0, payload.length);
        return new ExtractedStegoPayload(header.contentType(), header.encrypted(), payload);
    }

    @Override
    public byte[] extractPayload(byte[] bmpBytes) {
        ExtractedStegoPayload extracted = extract(bmpBytes);
        requirePlain(extracted);
        return extracted.payload();
    }

    @Override
    public byte[] hideText(byte[] bmpBytes, String message) {
        Objects.requireNonNull(message, "message");
        return hide(bmpBytes, message.getBytes(StandardCharsets.UTF_8), StegoContentType.UTF8_TEXT);
    }

    @Override
    public String extractText(byte[] bmpBytes) {
        ExtractedStegoPayload extracted = extract(bmpBytes);
        requirePlain(extracted);
        return extracted.asUtf8String();
    }

    @Override
    public byte[] hideEncryptedPayload(byte[] bmpBytes, byte[] payload, char[] password) {
        return hideEncrypted(bmpBytes, payload, StegoContentType.BINARY, password);
    }

    @Override
    public byte[] extractEncryptedPayload(byte[] bmpBytes, char[] password) {
        return extractEncrypted(bmpBytes, password, StegoContentType.BINARY);
    }

    @Override
    public byte[] hideEncryptedText(byte[] bmpBytes, String message, char[] password) {
        Objects.requireNonNull(message, "message");
        return hideEncrypted(bmpBytes, message.getBytes(StandardCharsets.UTF_8), StegoContentType.UTF8_TEXT, password);
    }

    @Override
    public String extractEncryptedText(byte[] bmpBytes, char[] password) {
        byte[] decrypted = extractEncrypted(bmpBytes, password, StegoContentType.UTF8_TEXT);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private byte[] hideEncrypted(byte[] bmpBytes, byte[] payload, StegoContentType contentType, char[] password) {
        Objects.requireNonNull(payload, "payload");
        requirePassword(password);
        PasswordEncryptedData encryptedData = passwordFileCryptoWorkflow.encrypt(payload, password);
        return hideInternal(bmpBytes, serialize(encryptedData), contentType, true);
    }

    private byte[] extractEncrypted(byte[] bmpBytes, char[] password, StegoContentType expectedContentType) {
        requirePassword(password);
        ExtractedStegoPayload extracted = extract(bmpBytes);
        if (!extracted.encrypted()) {
            throw new IllegalStateException("Extracted payload is not encrypted");
        }
        if (extracted.contentType() != expectedContentType) {
            throw new IllegalStateException("Unexpected encrypted payload type: " + extracted.contentType());
        }
        PasswordEncryptedData encryptedData = deserialize(extracted.payload());
        return passwordFileCryptoWorkflow.decrypt(encryptedData, password);
    }

    private byte[] hideInternal(byte[] bmpBytes, byte[] payload, StegoContentType contentType, boolean encrypted) {
        Objects.requireNonNull(payload, "payload");
        BmpImage image = BmpImage.parse(bmpBytes);
        int maxPayloadBytes = image.maxPayloadBytes(StegoHeader.BYTE_LENGTH);
        if (payload.length > maxPayloadBytes) {
            throw new IllegalArgumentException("Payload is too large for this BMP: "
                    + payload.length + " bytes requested, " + maxPayloadBytes + " bytes available");
        }
        StegoHeader header = new StegoHeader(contentType, encrypted, payload.length);
        byte[] hiddenBytes = ByteBuffer.allocate(StegoHeader.BYTE_LENGTH + payload.length)
                .put(header.write())
                .put(payload)
                .array();
        byte[] result = bmpBytes.clone();
        image.writeLeastSignificantBits(result, hiddenBytes);
        return result;
    }

    private void requirePlain(ExtractedStegoPayload extracted) {
        if (extracted.encrypted()) {
            throw new IllegalStateException("Extracted payload is encrypted; use encrypted extraction workflow");
        }
    }

    private void requirePassword(char[] password) {
        Objects.requireNonNull(password, "password");
        if (password.length == 0) {
            throw new IllegalArgumentException("password must not be empty");
        }
    }

    private byte[] serialize(PasswordEncryptedData encryptedData) {
        byte[] salt = encryptedData.salt();
        byte[] iv = encryptedData.iv();
        byte[] cipherText = encryptedData.cipherText();
        return ByteBuffer.allocate(Integer.BYTES * 3 + salt.length + iv.length + cipherText.length)
                .putInt(salt.length)
                .putInt(iv.length)
                .putInt(cipherText.length)
                .put(salt)
                .put(iv)
                .put(cipherText)
                .array();
    }

    private PasswordEncryptedData deserialize(byte[] serialized) {
        Objects.requireNonNull(serialized, "serialized");
        if (serialized.length < Integer.BYTES * 3) {
            throw new IllegalArgumentException("Encrypted payload is too short");
        }
        ByteBuffer buffer = ByteBuffer.wrap(serialized);
        int saltLength = readLength(buffer, "saltLength");
        int ivLength = readLength(buffer, "ivLength");
        int cipherTextLength = readLength(buffer, "cipherTextLength");
        int expectedLength = Integer.BYTES * 3 + saltLength + ivLength + cipherTextLength;
        if (expectedLength != serialized.length) {
            throw new IllegalArgumentException("Invalid encrypted payload length");
        }
        byte[] salt = new byte[saltLength];
        byte[] iv = new byte[ivLength];
        byte[] cipherText = new byte[cipherTextLength];
        buffer.get(salt);
        buffer.get(iv);
        buffer.get(cipherText);
        return new PasswordEncryptedData(salt, iv, cipherText);
    }

    private int readLength(ByteBuffer buffer, String name) {
        int length = buffer.getInt();
        if (length < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        return length;
    }
}
