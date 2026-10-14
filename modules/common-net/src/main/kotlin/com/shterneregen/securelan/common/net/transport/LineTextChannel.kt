package com.shterneregen.securelan.common.net.transport

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.Closeable
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import java.nio.charset.StandardCharsets

/**
 * Text-based channel for reading and writing newline-delimited UTF-8 lines over a TCP [Socket].
 *
 * Line write operations are synchronized on the writer.
 *
 * @param socket the underlying TCP socket.
 */
class LineTextChannel @JvmOverloads @Throws(IOException::class) constructor(
    private val socket: Socket,
    private val maxLineCharacters: Int = 90_000,
) : Closeable {
    private val reader: BufferedReader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
    private val writer: BufferedWriter = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))

    /**
     * Reads a single line of text from the socket.
     *
     * @return the line read, without line-termination characters, or `null` if the end of stream has been reached.
     * @throws IOException if an I/O error occurs or the read timeout expires.
     */
    @Throws(IOException::class)
    fun readLine(maxCharacters: Int = maxLineCharacters): String? {
        val first = reader.read()
        if (first < 0) return null
        return SocketDeadlines.within(socket, 10_000) {
            val line = StringBuilder()
            var next = first
            while (next >= 0 && next != '\n'.code) {
                if (line.length >= minOf(maxCharacters, maxLineCharacters)) throw IOException("Line exceeds maximum size")
                line.append(next.toChar())
                next = reader.read()
            }
            if (next < 0) throw IOException("Incomplete line")
            line.toString().removeSuffix("\r")
        }
    }

    /**
     * Sets the socket read timeout in milliseconds (`SO_TIMEOUT`).
     *
     * @param timeoutMillis the timeout in milliseconds, or `0` for infinite timeout.
     * @throws IllegalArgumentException if [timeoutMillis] is negative.
     * @throws IOException if a socket option error occurs.
     */
    @Throws(IOException::class)
    fun setReadTimeout(timeoutMillis: Int) {
        require(timeoutMillis >= 0) { "Read timeout must not be negative" }
        socket.soTimeout = timeoutMillis
    }

    /**
     * Writes a line of text followed by a newline delimiter and flushes the buffer.
     *
     * @param line the text line to send.
     * @throws IOException if an I/O error occurs.
     */
    @Throws(IOException::class)
    fun writeLine(line: String) {
        if (line.length > maxLineCharacters || line.contains('\n') || line.contains('\r')) {
            throw IOException("Invalid line size or delimiter")
        }
        synchronized(writer) {
            SocketDeadlines.within(socket, 10_000) {
                writer.write(line)
                writer.newLine()
                writer.flush()
            }
        }
    }

    /**
     * Returns the remote socket address string, or an empty string if unconnected.
     */
    fun remoteAddress(): String = socket.remoteSocketAddress?.toString().orEmpty()

    /**
     * Closes the underlying socket and associated readers/writers.
     */
    @Throws(IOException::class)
    override fun close() {
        socket.close()
    }
}
