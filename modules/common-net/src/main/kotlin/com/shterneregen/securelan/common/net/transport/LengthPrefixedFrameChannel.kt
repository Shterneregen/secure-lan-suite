package com.shterneregen.securelan.common.net.transport

import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket
import java.util.Objects

class LengthPrefixedFrameChannel @Throws(IOException::class) constructor(
    private val socket: Socket,
    private val maxFrameSizeBytes: Int,
) : Closeable {
    private val input: DataInputStream
    private val output: DataOutputStream

    @Throws(IOException::class)
    constructor(socket: Socket) : this(socket, DEFAULT_MAX_FRAME_SIZE_BYTES)

    init {
        Objects.requireNonNull(socket, "socket")
        require(maxFrameSizeBytes >= 1) { "maxFrameSizeBytes must be positive" }
        input = DataInputStream(socket.getInputStream())
        output = DataOutputStream(socket.getOutputStream())
    }

    @Throws(IOException::class)
    fun writeUtf(value: String) {
        Objects.requireNonNull(value, "value")
        synchronized(output) {
            output.writeUTF(value)
            output.flush()
        }
    }

    @Throws(IOException::class)
    fun readUtf(): String = input.readUTF()

    @Throws(IOException::class)
    fun writeFrame(bytes: ByteArray) {
        Objects.requireNonNull(bytes, "bytes")
        if (bytes.size > maxFrameSizeBytes) {
            throw IOException("Frame exceeds maximum size: ${bytes.size} > $maxFrameSizeBytes")
        }
        synchronized(output) {
            output.writeInt(bytes.size)
            output.write(bytes)
            output.flush()
        }
    }

    @Throws(IOException::class)
    fun readFrame(): ByteArray {
        val length = input.readInt()
        if (length < 0) {
            throw IOException("Negative frame length")
        }
        if (length > maxFrameSizeBytes) {
            throw IOException("Frame exceeds maximum size: $length > $maxFrameSizeBytes")
        }
        val bytes = input.readNBytes(length)
        if (bytes.size != length) {
            throw IOException("Unexpected end of stream")
        }
        return bytes
    }

    fun remoteAddress(): String = socket.remoteSocketAddress.toString()

    @Throws(IOException::class)
    override fun close() {
        socket.close()
    }

    companion object {
        const val DEFAULT_MAX_FRAME_SIZE_BYTES: Int = 128 * 1024 * 1024
    }
}
