package com.shterneregen.securelan.webrtc.event

import java.util.Objects

class RtcVideoFrameEvent(
    sessionId: String,
    peer: String?,
    private val local: Boolean,
    private val width: Int,
    private val height: Int,
    private val rotation: Int,
    bgraPixels: ByteArray,
) : RtcEvent {
    private val sessionId: String = Objects.requireNonNull(sessionId, "sessionId must not be null")
    private val peer: String = peer ?: ""
    private val bgraPixels: ByteArray

    init {
        require(width > 0 && height > 0) { "Video frame dimensions must be positive: ${width}x${height}" }

        val copiedPixels = Objects.requireNonNull(bgraPixels, "bgraPixels must not be null").copyOf()
        val expectedBgraLength = Math.multiplyExact(Math.multiplyExact(width, height), 4)
        require(copiedPixels.size == expectedBgraLength) {
            "BGRA payload length ${copiedPixels.size} does not match frame dimensions ${width}x${height}"
        }
        this.bgraPixels = copiedPixels
    }

    fun sessionId(): String = sessionId

    fun peer(): String = peer


    fun local(): Boolean = local

    fun width(): Int = width

    fun height(): Int = height
    fun rotation(): Int = rotation

    fun bgraPixels(): ByteArray = bgraPixels.copyOf()

    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is RtcVideoFrameEvent &&
                sessionId == other.sessionId &&
                peer == other.peer &&
                local == other.local &&
                width == other.width &&
                height == other.height &&
                rotation == other.rotation &&
                bgraPixels == other.bgraPixels)

    override fun hashCode(): Int = Objects.hash(sessionId, peer, local, width, height, rotation, bgraPixels)

    override fun toString(): String =
        "RtcVideoFrameEvent[sessionId=$sessionId, peer=$peer, local=$local, width=$width, height=$height, rotation=$rotation, bgraPixels=$bgraPixels]"
}
