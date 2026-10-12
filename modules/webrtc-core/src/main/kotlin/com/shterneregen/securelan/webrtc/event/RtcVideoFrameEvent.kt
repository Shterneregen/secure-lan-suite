package com.shterneregen.securelan.webrtc.event

import java.util.Objects

/**
 * Event carrying raw BGRA pixel buffers for local camera previews or incoming remote video tracks.
 *
 * Each frame represents an uncompressed, 32-bit BGRA image (4 bytes per pixel) ready for rendering
 * in desktop or mobile UI layers without additional native decoding.
 *
 * @param sessionId identifier of the active WebRTC session or camera preview.
 * @param peer peer nickname or device name associated with the video frame.
 * @param local `true` if this frame originated from the local camera; `false` if received from remote peer.
 * @param width horizontal frame dimension in pixels (must be positive).
 * @param height vertical frame dimension in pixels (must be positive).
 * @param rotation clockwise frame orientation in degrees (typically 0, 90, 180, or 270).
 * @param bgraPixels uncompressed 32-bit BGRA byte buffer (length must equal `width * height * 4`).
 */
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

    /** Returns the unique session identifier. */
    fun sessionId(): String = sessionId

    /** Returns the peer or device identifier associated with this video stream. */
    fun peer(): String = peer

    /** Returns `true` if this frame is from a local capture source, or `false` for remote streams. */
    fun local(): Boolean = local

    /** Returns the frame width in pixels. */
    fun width(): Int = width

    /** Returns the frame height in pixels. */
    fun height(): Int = height

    /** Returns the frame rotation angle in degrees clockwise. */
    fun rotation(): Int = rotation

    /** Returns a defensive copy of the raw 32-bit BGRA pixel bytes. */
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
                bgraPixels.contentEquals(other.bgraPixels))

    override fun hashCode(): Int =
        Objects.hash(sessionId, peer, local, width, height, rotation, bgraPixels.contentHashCode())

    override fun toString(): String =
        "RtcVideoFrameEvent[sessionId=$sessionId, peer=$peer, local=$local, width=$width, height=$height, rotation=$rotation, bgraPixels=${bgraPixels.size}B]"
}
