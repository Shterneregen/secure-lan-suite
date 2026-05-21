package com.shterneregen.securelan.stego

import com.shterneregen.securelan.stego.model.StegoContentType
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

class BmpSteganographyServiceTest {
    private val steganographyService = StegoServices.createDefault().steganographyService()

    @Test
    fun shouldCalculateBmpCapacity() {
        val bmp = createBmp(8, 8, 24)

        val capacity = steganographyService.inspect(bmp)

        assertEquals(8, capacity.width)
        assertEquals(8, capacity.height)
        assertEquals(24, capacity.bitsPerPixel)
        assertEquals(192, capacity.carrierBytes)
        assertEquals(12, capacity.headerBytes)
        assertEquals(12, capacity.payloadCapacityBytes)
    }

    @Test
    fun shouldHideAndExtractBinaryPayloadFromBmp() {
        val bmp = createBmp(32, 32, 24)
        val payload = "binary payload".toByteArray(StandardCharsets.UTF_8)

        val stegoBmp = steganographyService.hidePayload(bmp, payload)
        val extracted = steganographyService.extractPayload(stegoBmp)

        assertArrayEquals(payload, extracted)
        assertEquals('B'.code.toByte(), stegoBmp[0])
        assertEquals('M'.code.toByte(), stegoBmp[1])
    }

    @Test
    fun shouldHideAndExtractTextPayloadFromBmp() {
        val bmp = createBmp(32, 32, 32)
        val message = "Привет, SecureLanSuite stego!"

        val stegoBmp = steganographyService.hideText(bmp, message)

        assertEquals(message, steganographyService.extractText(stegoBmp))
        val extracted = steganographyService.extract(stegoBmp)
        assertEquals(StegoContentType.UTF8_TEXT, extracted.contentType())
        assertFalse(extracted.encrypted())
    }

    @Test
    fun shouldEncryptHideExtractAndDecryptPayload() {
        val bmp = createBmp(96, 96, 24)
        val payload = "classified payload".toByteArray(StandardCharsets.UTF_8)
        val password = "strong-password".toCharArray()

        val stegoBmp = steganographyService.hideEncryptedPayload(bmp, payload, password)

        assertArrayEquals(payload, steganographyService.extractEncryptedPayload(stegoBmp, password))
        assertTrue(steganographyService.extract(stegoBmp).encrypted())
    }

    @Test
    fun shouldEncryptHideExtractAndDecryptText() {
        val bmp = createBmp(96, 96, 32)
        val message = "hidden encrypted text"
        val password = "text-password".toCharArray()

        val stegoBmp = steganographyService.hideEncryptedText(bmp, message, password)

        assertEquals(message, steganographyService.extractEncryptedText(stegoBmp, password))
    }

    @Test
    fun shouldRejectWrongPasswordForEncryptedPayload() {
        val bmp = createBmp(96, 96, 24)
        val stegoBmp = steganographyService.hideEncryptedPayload(
            bmp,
            "secret".toByteArray(StandardCharsets.UTF_8),
            "correct".toCharArray(),
        )

        assertThrows(IllegalStateException::class.java) {
            steganographyService.extractEncryptedPayload(stegoBmp, "wrong".toCharArray())
        }
    }

    @Test
    fun shouldRejectOversizedPayload() {
        val bmp = createBmp(4, 4, 24)
        val payload = ByteArray(64)

        assertThrows(IllegalArgumentException::class.java) { steganographyService.hidePayload(bmp, payload) }
    }

    private fun createBmp(width: Int, height: Int, bitsPerPixel: Int): ByteArray {
        val bytesPerPixel = bitsPerPixel / 8
        val rowStride = ((width * bitsPerPixel + 31) / 32) * 4
        val pixelDataOffset = 54
        val imageSize = rowStride * height
        val fileSize = pixelDataOffset + imageSize
        val bmp = ByteArray(fileSize)
        val buffer = ByteBuffer.wrap(bmp).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(0, 'B'.code.toByte())
        buffer.put(1, 'M'.code.toByte())
        buffer.putInt(2, fileSize)
        buffer.putInt(10, pixelDataOffset)
        buffer.putInt(14, 40)
        buffer.putInt(18, width)
        buffer.putInt(22, height)
        buffer.putShort(26, 1.toShort())
        buffer.putShort(28, bitsPerPixel.toShort())
        buffer.putInt(30, 0)
        buffer.putInt(34, imageSize)

        for (row in 0 until height) {
            val rowStart = pixelDataOffset + row * rowStride
            for (x in 0 until width) {
                val pixelStart = rowStart + x * bytesPerPixel
                bmp[pixelStart] = (0x80 + ((x + row) and 0x3F)).toByte()
                bmp[pixelStart + 1] = (0x90 + ((x * 3 + row) and 0x3F)).toByte()
                bmp[pixelStart + 2] = (0xA0 + ((x + row * 3) and 0x3F)).toByte()
                if (bytesPerPixel == 4) {
                    bmp[pixelStart + 3] = 0xFF.toByte()
                }
            }
        }
        return bmp
    }
}
