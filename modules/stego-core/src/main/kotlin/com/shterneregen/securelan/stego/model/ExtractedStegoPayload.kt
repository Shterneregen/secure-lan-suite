package com.shterneregen.securelan.stego.model

import java.nio.charset.StandardCharsets
import java.util.Objects

class ExtractedStegoPayload(
    contentType: StegoContentType,
    private val encrypted: Boolean,
    payload: ByteArray,
) {
    private val contentType: StegoContentType = Objects.requireNonNull(contentType, "contentType")
    private val payload: ByteArray = Objects.requireNonNull(payload, "payload").clone()

    fun contentType(): StegoContentType = contentType

    fun encrypted(): Boolean = encrypted

    fun payload(): ByteArray = payload.clone()

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
                payload == other.payload)

    override fun hashCode(): Int = Objects.hash(contentType, encrypted, payload)

    override fun toString(): String = "ExtractedStegoPayload[contentType=$contentType, encrypted=$encrypted, payload=${payload.size} bytes]"
}
