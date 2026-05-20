package com.shterneregen.securelan.stego.service.impl

import com.shterneregen.securelan.crypto.model.PasswordEncryptedData
import com.shterneregen.securelan.crypto.workflow.PasswordFileCryptoWorkflow
import com.shterneregen.securelan.stego.model.BmpCapacity
import com.shterneregen.securelan.stego.model.ExtractedStegoPayload
import com.shterneregen.securelan.stego.model.StegoContentType
import com.shterneregen.securelan.stego.service.SteganographyService
import com.shterneregen.securelan.stego.service.impl.internal.BmpImage
import com.shterneregen.securelan.stego.service.impl.internal.StegoHeader
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.Objects

class BmpSteganographyService(
    private val passwordFileCryptoWorkflow: PasswordFileCryptoWorkflow,
) : SteganographyService {
    init {
        Objects.requireNonNull(passwordFileCryptoWorkflow, "passwordFileCryptoWorkflow")
    }

    override fun inspect(bmpBytes: ByteArray): BmpCapacity {
        val image = BmpImage.parse(bmpBytes)
        return BmpCapacity(
            image.width,
            image.height,
            image.bitsPerPixel,
            image.carrierByteCount,
            StegoHeader.BYTE_LENGTH,
            image.maxPayloadBytes(StegoHeader.BYTE_LENGTH),
        )
    }

    override fun hide(bmpBytes: ByteArray, payload: ByteArray, contentType: StegoContentType): ByteArray {
        Objects.requireNonNull(contentType, "contentType")
        return hideInternal(bmpBytes, payload, contentType, false)
    }

    override fun extract(bmpBytes: ByteArray): ExtractedStegoPayload {
        val image = BmpImage.parse(bmpBytes)
        val headerBytes = image.readLeastSignificantBits(bmpBytes, StegoHeader.BYTE_LENGTH)
        val header = StegoHeader.read(headerBytes)
        val maxPayloadBytes = image.maxPayloadBytes(StegoHeader.BYTE_LENGTH)
        if (header.payloadLength > maxPayloadBytes) {
            throw IllegalArgumentException("Hidden payload length exceeds BMP capacity")
        }
        val hiddenBytes = image.readLeastSignificantBits(bmpBytes, StegoHeader.BYTE_LENGTH + header.payloadLength)
        val payload = ByteArray(header.payloadLength)
        System.arraycopy(hiddenBytes, StegoHeader.BYTE_LENGTH, payload, 0, payload.size)
        return ExtractedStegoPayload(header.contentType, header.encrypted, payload)
    }

    override fun extractPayload(bmpBytes: ByteArray): ByteArray {
        val extracted = extract(bmpBytes)
        requirePlain(extracted)
        return extracted.payload()
    }

    override fun hideText(bmpBytes: ByteArray, message: String): ByteArray {
        Objects.requireNonNull(message, "message")
        return hide(bmpBytes, message.toByteArray(StandardCharsets.UTF_8), StegoContentType.UTF8_TEXT)
    }

    override fun extractText(bmpBytes: ByteArray): String {
        val extracted = extract(bmpBytes)
        requirePlain(extracted)
        return extracted.asUtf8String()
    }

    override fun hideEncryptedPayload(bmpBytes: ByteArray, payload: ByteArray, password: CharArray): ByteArray =
        hideEncrypted(bmpBytes, payload, StegoContentType.BINARY, password)

    override fun extractEncryptedPayload(bmpBytes: ByteArray, password: CharArray): ByteArray =
        extractEncrypted(bmpBytes, password, StegoContentType.BINARY)

    override fun hideEncryptedText(bmpBytes: ByteArray, message: String, password: CharArray): ByteArray {
        Objects.requireNonNull(message, "message")
        return hideEncrypted(bmpBytes, message.toByteArray(StandardCharsets.UTF_8), StegoContentType.UTF8_TEXT, password)
    }

    override fun extractEncryptedText(bmpBytes: ByteArray, password: CharArray): String =
        String(extractEncrypted(bmpBytes, password, StegoContentType.UTF8_TEXT), StandardCharsets.UTF_8)

    private fun hideEncrypted(bmpBytes: ByteArray, payload: ByteArray, contentType: StegoContentType, password: CharArray): ByteArray {
        Objects.requireNonNull(payload, "payload")
        requirePassword(password)
        val encryptedData = passwordFileCryptoWorkflow.encrypt(payload, password)
        return hideInternal(bmpBytes, serialize(encryptedData), contentType, true)
    }

    private fun extractEncrypted(bmpBytes: ByteArray, password: CharArray, expectedContentType: StegoContentType): ByteArray {
        requirePassword(password)
        val extracted = extract(bmpBytes)
        if (!extracted.encrypted()) {
            throw IllegalStateException("Extracted payload is not encrypted")
        }
        if (extracted.contentType() != expectedContentType) {
            throw IllegalStateException("Unexpected encrypted payload type: ${extracted.contentType()}")
        }
        val encryptedData = deserialize(extracted.payload())
        return passwordFileCryptoWorkflow.decrypt(encryptedData, password)
    }

    private fun hideInternal(bmpBytes: ByteArray, payload: ByteArray, contentType: StegoContentType, encrypted: Boolean): ByteArray {
        Objects.requireNonNull(payload, "payload")
        val image = BmpImage.parse(bmpBytes)
        val maxPayloadBytes = image.maxPayloadBytes(StegoHeader.BYTE_LENGTH)
        if (payload.size > maxPayloadBytes) {
            throw IllegalArgumentException("Payload is too large for this BMP: ${payload.size} bytes requested, $maxPayloadBytes bytes available")
        }
        val header = StegoHeader(contentType, encrypted, payload.size)
        val hiddenBytes = ByteBuffer.allocate(StegoHeader.BYTE_LENGTH + payload.size)
            .put(header.write())
            .put(payload)
            .array()
        val result = bmpBytes.clone()
        image.writeLeastSignificantBits(result, hiddenBytes)
        return result
    }

    private fun requirePlain(extracted: ExtractedStegoPayload) {
        if (extracted.encrypted()) {
            throw IllegalStateException("Extracted payload is encrypted; use encrypted extraction workflow")
        }
    }

    private fun requirePassword(password: CharArray) {
        Objects.requireNonNull(password, "password")
        if (password.isEmpty()) {
            throw IllegalArgumentException("password must not be empty")
        }
    }

    private fun serialize(encryptedData: PasswordEncryptedData): ByteArray {
        val salt = encryptedData.salt()
        val iv = encryptedData.iv()
        val cipherText = encryptedData.cipherText()
        return ByteBuffer.allocate(Int.SIZE_BYTES * 3 + salt.size + iv.size + cipherText.size)
            .putInt(salt.size)
            .putInt(iv.size)
            .putInt(cipherText.size)
            .put(salt)
            .put(iv)
            .put(cipherText)
            .array()
    }

    private fun deserialize(serialized: ByteArray): PasswordEncryptedData {
        Objects.requireNonNull(serialized, "serialized")
        if (serialized.size < Int.SIZE_BYTES * 3) {
            throw IllegalArgumentException("Encrypted payload is too short")
        }
        val buffer = ByteBuffer.wrap(serialized)
        val saltLength = readLength(buffer, "saltLength")
        val ivLength = readLength(buffer, "ivLength")
        val cipherTextLength = readLength(buffer, "cipherTextLength")
        val expectedLength = Int.SIZE_BYTES * 3 + saltLength + ivLength + cipherTextLength
        if (expectedLength != serialized.size) {
            throw IllegalArgumentException("Invalid encrypted payload length")
        }
        val salt = ByteArray(saltLength)
        val iv = ByteArray(ivLength)
        val cipherText = ByteArray(cipherTextLength)
        buffer.get(salt)
        buffer.get(iv)
        buffer.get(cipherText)
        return PasswordEncryptedData(salt, iv, cipherText)
    }

    private fun readLength(buffer: ByteBuffer, name: String): Int {
        val length = buffer.getInt()
        if (length < 0) {
            throw IllegalArgumentException("$name must not be negative")
        }
        return length
    }
}
