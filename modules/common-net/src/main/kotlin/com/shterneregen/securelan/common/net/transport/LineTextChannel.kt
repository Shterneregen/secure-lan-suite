package com.shterneregen.securelan.common.net.transport

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.Closeable
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.Objects

class LineTextChannel @Throws(IOException::class) constructor(private val socket: Socket) : Closeable {
    private val reader: BufferedReader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
    private val writer: BufferedWriter = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))

    init {
        Objects.requireNonNull(socket, "socket")
    }

    @Throws(IOException::class)
    fun readLine(): String? = reader.readLine()

    @Throws(IOException::class)
    fun writeLine(line: String) {
        Objects.requireNonNull(line, "line")
        synchronized(writer) {
            writer.write(line)
            writer.newLine()
            writer.flush()
        }
    }

    fun remoteAddress(): String = socket.remoteSocketAddress.toString()

    @Throws(IOException::class)
    override fun close() {
        socket.close()
    }
}
