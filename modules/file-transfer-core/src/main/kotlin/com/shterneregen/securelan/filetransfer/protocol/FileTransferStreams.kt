package com.shterneregen.securelan.filetransfer.protocol

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** Shared V2 stream rules. Callers own streams, deadlines, publication, quotas and cancellation. */
object FileTransferStreams {
    fun send(input: InputStream, size: Long, write: (ByteArray) -> Unit, checkOpen: () -> Unit = {}, progress: (Long) -> Unit = {}): Long {
        require(size >= 0)
        val buffer = ByteArray(16 * 1024)
        var sent = 0L
        while (true) {
            checkOpen()
            val read = try { input.read(buffer) } catch (error: IOException) { throw TransferSourceException("Cannot read source", error) }
            if (read == -1) break
            if (read == 0) continue
            if (read.toLong() > size - sent) throw TransferSourceException("Source size changed")
            write(buffer.copyOf(read))
            sent += read
            progress(sent)
        }
        if (sent != size) throw TransferSourceException("Source size changed")
        return sent
    }

    fun receive(output: OutputStream, size: Long, read: () -> ByteArray, progress: (Long) -> Unit = {}): Long {
        require(size >= 0)
        var received = 0L
        while (true) {
            val chunk = read()
            if (chunk.isEmpty()) break
            if (chunk.size.toLong() > size - received) throw IOException("Received file exceeds declared size")
            output.write(chunk)
            received += chunk.size
            progress(received)
        }
        if (received != size) throw IOException("Received file length differs from declared size")
        return received
    }

    /** EOF may reach the receiver even if writing fails; mark the uncertain boundary before calling. */
    fun finishSending(session: FileTransferSession, beforeEof: () -> Unit, awaitingConfirmation: () -> Unit) {
        beforeEof()
        session.writeEncryptedBytes(ByteArray(0))
        awaitingConfirmation()
        if (session.readEncryptedText() != "DONE") throw IOException("Missing transfer confirmation")
    }

    /** Publication and quota commit must succeed before DONE is sent. */
    fun <T> publishAndConfirm(session: FileTransferSession, publish: () -> T, commit: () -> Unit): T {
        val target = publish()
        commit()
        session.writeEncryptedText("DONE")
        return target
    }
}

class TransferSourceException(message: String, cause: Throwable? = null) : IOException(message, cause)
