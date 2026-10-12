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
class LineTextChannel @Throws(IOException::class) constructor(private val socket: Socket) : Closeable {
    private val reader: BufferedReader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
    private val writer: BufferedWriter = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))

    /**
     * Reads a single line of text from the socket.
     *
     * @return the line read, without line-termination characters, or `null` if the end of stream has been reached.
     * @throws IOException if an I/O error occurs or the read timeout expires.
     */
    @Throws(IOException::class)
    fun readLine(): String? = reader.readLine()

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
        synchronized(writer) {
            writer.write(line)
            writer.newLine()
            writer.flush()
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
