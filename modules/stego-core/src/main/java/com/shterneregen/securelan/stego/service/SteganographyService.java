package com.shterneregen.securelan.stego.service;

import com.shterneregen.securelan.stego.model.BmpCapacity;
import com.shterneregen.securelan.stego.model.ExtractedStegoPayload;
import com.shterneregen.securelan.stego.model.StegoContentType;

public interface SteganographyService {
    BmpCapacity inspect(byte[] bmpBytes);

    byte[] hide(byte[] bmpBytes, byte[] payload, StegoContentType contentType);

    ExtractedStegoPayload extract(byte[] bmpBytes);

    default byte[] hidePayload(byte[] bmpBytes, byte[] payload) {
        return hide(bmpBytes, payload, StegoContentType.BINARY);
    }

    byte[] extractPayload(byte[] bmpBytes);

    byte[] hideText(byte[] bmpBytes, String message);

    String extractText(byte[] bmpBytes);

    byte[] hideEncryptedPayload(byte[] bmpBytes, byte[] payload, char[] password);

    byte[] extractEncryptedPayload(byte[] bmpBytes, char[] password);

    byte[] hideEncryptedText(byte[] bmpBytes, String message, char[] password);

    String extractEncryptedText(byte[] bmpBytes, char[] password);
}
