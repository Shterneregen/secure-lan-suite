package com.shterneregen.securelan.stego.model

import java.nio.charset.StandardCharsets
import java.util.Objects

/**
 * Encapsulates an extracted steganography payload and its associated metadata.
 *
 * Internal byte arrays are defensively cloned upon construction and retrieval to preserve immutability.
 *
 * @param contentType The type of embedded content ([StegoContentType.BINARY] or [StegoContentType.UTF8_TEXT]).
 * @param encrypted Whether the payload was encrypted prior to embedding.
 * @param payload The raw extracted payload bytes.
 */
class ExtractedStegoPayload(
    contentType: StegoContentType,
    private val encrypted: Boolean,
    payload: ByteArray,
) {
    private val contentType: StegoContentType = Objects.requireNonNull(contentType, "contentType")
    private val payload: ByteArray = Objects.requireNonNull(payload, "payload").clone()

    /**
     * Returns the content type of the extracted payload.
     *
     * @return The [StegoContentType].
     */
    fun contentType(): StegoContentType = contentType

    /**
     * Indicates whether the payload is encrypted.
     *
     * @return `true` if the payload was embedded with encryption; `false` otherwise.
     */
    fun encrypted(): Boolean = encrypted

    /**
     * Returns a defensive copy of the raw extracted payload bytes.
     *
     * @return Cloned byte array containing the payload bytes.
     */
    fun payload(): ByteArray = payload.clone()

    /**
     * Decodes and returns the payload as a UTF-8 string.
     *
     * @return The payload decoded as a UTF-8 string.
     * @throws IllegalStateException If [contentType] is not [StegoContentType.UTF8_TEXT].
     */
    fun asUtf8String(): String {
        if (contentType != StegoContentType.UTF8_TEXT) {
            throw IllegalStateException("Extracted payload is not UTF-8 text")
        }
        return String(payload, StandardCharsets.UTF_8)
    }

    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is ExtractedStegoPayload &&
                contentType == other.contentType &&
                encrypted == other.encrypted &&
                payload.contentEquals(other.payload))

    override fun hashCode(): Int {
        var result = contentType.hashCode()
        result = 31 * result + encrypted.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }

    override fun toString(): String =
        "ExtractedStegoPayload[contentType=$contentType, encrypted=$encrypted, payload=${payload.size} bytes]"
}
