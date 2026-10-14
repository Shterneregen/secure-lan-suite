package com.shterneregen.securelan.common.net.transport

import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.net.Socket

/**
 * Bidirectional framing channel for exchanging UTF strings and length-prefixed binary frames over a TCP [Socket].
 *
 * Wire format for binary frames:
 * - 4-byte big-endian integer length prefix specifying the payload size in bytes.
 * - Followed immediately by the raw payload bytes.
 *
 * Wire format for UTF strings:
 * - Standard Java [DataOutputStream.writeUTF] / [DataInputStream.readUTF] modified UTF-8 encoding.
 *
 * Write operations are synchronized on the output stream. Read operations are synchronized on the input stream.
 *
 * @param socket the underlying TCP socket.
 * @param maxFrameSizeBytes maximum allowable frame payload size in bytes.
 */
class LengthPrefixedFrameChannel @Throws(IOException::class) constructor(
    private val socket: Socket,
    private val maxFrameSizeBytes: Int,
) : Closeable {
    private val input: DataInputStream
    private val output: DataOutputStream

    /**
     * Creates a channel with the default maximum frame size ([DEFAULT_MAX_FRAME_SIZE_BYTES]).
     *
     * @param socket the underlying TCP socket.
     */
    @Throws(IOException::class)
    constructor(socket: Socket) : this(socket, DEFAULT_MAX_FRAME_SIZE_BYTES)

    init {
        require(maxFrameSizeBytes >= 1) { "maxFrameSizeBytes must be positive" }
        input = DataInputStream(socket.getInputStream())
        output = DataOutputStream(socket.getOutputStream())
    }

    /**
     * Writes a UTF-8 string to the socket output stream using modified UTF-8 format.
     *
     * @param value the string to write.
     * @throws IOException if a network or I/O error occurs.
     */
    @Throws(IOException::class)
    fun writeUtf(value: String) {
        if (value.toByteArray(Charsets.UTF_8).size > 4096) throw IOException("Handshake field exceeds maximum size")
        synchronized(output) {
            output.writeUTF(value)
            output.flush()
        }
    }

    /**
     * Reads a UTF-8 string from the socket input stream.
     *
     * @return the decoded string.
     * @throws IOException if an I/O error occurs or the stream reaches end-of-file.
     */
    @Throws(IOException::class)
    fun readUtf(): String = synchronized(input) {
        val length = input.readUnsignedShort()
        if (length > 4096) throw IOException("Handshake field exceeds maximum size")
        val frame = ByteArray(length + 2)
        frame[0] = (length ushr 8).toByte()
        frame[1] = length.toByte()
        input.readFully(frame, 2, length)
        DataInputStream(frame.inputStream()).readUTF()
    }

    /**
     * Writes a length-prefixed binary frame to the socket output stream.
     *
     * @param bytes the binary payload to write.
     * @throws IOException if [bytes] exceeds [maxFrameSizeBytes] or an I/O error occurs.
     */
    @Throws(IOException::class)
    fun writeFrame(bytes: ByteArray) {
        if (bytes.size > maxFrameSizeBytes) {
            throw IOException("Frame exceeds maximum size: ${bytes.size} > $maxFrameSizeBytes")
        }
        synchronized(output) {
            output.writeInt(bytes.size)
            output.write(bytes)
            output.flush()
        }
    }

    /**
     * Reads a length-prefixed binary frame from the socket input stream.
     *
     * @return the received frame payload bytes.
     * @throws IOException if the frame length is negative, exceeds [maxFrameSizeBytes],
     *                     or end-of-stream is reached prematurely.
     */
    @Throws(IOException::class)
    fun readFrame(): ByteArray = synchronized(input) {
        val length = input.readInt()
        if (length < 0) {
            throw IOException("Negative frame length: $length")
        }
        if (length > maxFrameSizeBytes) {
            throw IOException("Frame exceeds maximum size: $length > $maxFrameSizeBytes")
        }
        val bytes = ByteArray(length)
        try {
            input.readFully(bytes)
        } catch (e: EOFException) {
            throw IOException("Unexpected end of stream", e)
        }
        bytes
    }

    /**
     * Returns the remote socket address description, or an empty string if unconnected.
     */
    fun remoteAddress(): String = socket.remoteSocketAddress?.toString().orEmpty()

    /**
     * Closes the underlying socket and associated streams.
     */
    @Throws(IOException::class)
    override fun close() {
        socket.close()
    }

    companion object {
        /**
         * Default maximum allowed encrypted record size (64 KiB plus header/tag).
         */
        const val DEFAULT_MAX_FRAME_SIZE_BYTES: Int = 64 * 1024 + 25
    }
}
