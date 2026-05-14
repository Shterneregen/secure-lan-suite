package com.shterneregen.securelan.stego;

import com.shterneregen.securelan.stego.model.StegoContentType;
import com.shterneregen.securelan.stego.service.SteganographyService;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BmpSteganographyServiceTest {
    private final SteganographyService steganographyService = StegoServices.createDefault().steganographyService();

    @Test
    void shouldCalculateBmpCapacity() {
        byte[] bmp = createBmp(8, 8, 24);

        var capacity = steganographyService.inspect(bmp);

        assertEquals(8, capacity.width());
        assertEquals(8, capacity.height());
        assertEquals(24, capacity.bitsPerPixel());
        assertEquals(192, capacity.carrierBytes());
        assertEquals(12, capacity.headerBytes());
        assertEquals(12, capacity.payloadCapacityBytes());
    }

    @Test
    void shouldHideAndExtractBinaryPayloadFromBmp() {
        byte[] bmp = createBmp(32, 32, 24);
        byte[] payload = "binary payload".getBytes(StandardCharsets.UTF_8);

        byte[] stegoBmp = steganographyService.hidePayload(bmp, payload);
        byte[] extracted = steganographyService.extractPayload(stegoBmp);

        assertArrayEquals(payload, extracted);
        assertEquals('B', stegoBmp[0]);
        assertEquals('M', stegoBmp[1]);
    }

    @Test
    void shouldHideAndExtractTextPayloadFromBmp() {
        byte[] bmp = createBmp(32, 32, 32);
        String message = "Привет, SecureLanSuite stego!";

        byte[] stegoBmp = steganographyService.hideText(bmp, message);

        assertEquals(message, steganographyService.extractText(stegoBmp));
        var extracted = steganographyService.extract(stegoBmp);
        assertEquals(StegoContentType.UTF8_TEXT, extracted.contentType());
        assertFalse(extracted.encrypted());
    }

    @Test
    void shouldEncryptHideExtractAndDecryptPayload() {
        byte[] bmp = createBmp(96, 96, 24);
        byte[] payload = "classified payload".getBytes(StandardCharsets.UTF_8);
        char[] password = "strong-password".toCharArray();

        byte[] stegoBmp = steganographyService.hideEncryptedPayload(bmp, payload, password);

        assertArrayEquals(payload, steganographyService.extractEncryptedPayload(stegoBmp, password));
        assertTrue(steganographyService.extract(stegoBmp).encrypted());
    }

    @Test
    void shouldEncryptHideExtractAndDecryptText() {
        byte[] bmp = createBmp(96, 96, 32);
        String message = "hidden encrypted text";
        char[] password = "text-password".toCharArray();

        byte[] stegoBmp = steganographyService.hideEncryptedText(bmp, message, password);

        assertEquals(message, steganographyService.extractEncryptedText(stegoBmp, password));
    }

    @Test
    void shouldRejectWrongPasswordForEncryptedPayload() {
        byte[] bmp = createBmp(96, 96, 24);
        byte[] stegoBmp = steganographyService.hideEncryptedPayload(
                bmp,
                "secret".getBytes(StandardCharsets.UTF_8),
                "correct".toCharArray()
        );

        assertThrows(IllegalStateException.class,
                () -> steganographyService.extractEncryptedPayload(stegoBmp, "wrong".toCharArray()));
    }

    @Test
    void shouldRejectOversizedPayload() {
        byte[] bmp = createBmp(4, 4, 24);
        byte[] payload = new byte[64];

        assertThrows(IllegalArgumentException.class, () -> steganographyService.hidePayload(bmp, payload));
    }

    private byte[] createBmp(int width, int height, int bitsPerPixel) {
        int bytesPerPixel = bitsPerPixel / 8;
        int rowStride = ((width * bitsPerPixel + 31) / 32) * 4;
        int pixelDataOffset = 54;
        int imageSize = rowStride * height;
        int fileSize = pixelDataOffset + imageSize;
        byte[] bmp = new byte[fileSize];
        ByteBuffer buffer = ByteBuffer.wrap(bmp).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(0, (byte) 'B');
        buffer.put(1, (byte) 'M');
        buffer.putInt(2, fileSize);
        buffer.putInt(10, pixelDataOffset);
        buffer.putInt(14, 40);
        buffer.putInt(18, width);
        buffer.putInt(22, height);
        buffer.putShort(26, (short) 1);
        buffer.putShort(28, (short) bitsPerPixel);
        buffer.putInt(30, 0);
        buffer.putInt(34, imageSize);

        for (int row = 0; row < height; row++) {
            int rowStart = pixelDataOffset + row * rowStride;
            for (int x = 0; x < width; x++) {
                int pixelStart = rowStart + x * bytesPerPixel;
                bmp[pixelStart] = (byte) (0x80 + ((x + row) & 0x3F));
                bmp[pixelStart + 1] = (byte) (0x90 + ((x * 3 + row) & 0x3F));
                bmp[pixelStart + 2] = (byte) (0xA0 + ((x + row * 3) & 0x3F));
                if (bytesPerPixel == 4) {
                    bmp[pixelStart + 3] = (byte) 0xFF;
                }
            }
        }
        return bmp;
    }
}
