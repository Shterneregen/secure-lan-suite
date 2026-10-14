package com.shterneregen.securelan.filetransfer.protocol

import java.io.IOException

/** A receiver lifetime budget: completed files keep their charge; aborted partial files release it. */
class FileReceiveBudget(private val maxBytes: Long = 4L * 1024 * 1024 * 1024, private val maxFiles: Int = 256) {
    private var bytes = 0L
    private var files = 0

    init { require(maxBytes > 0 && maxFiles > 0) }

    @Synchronized fun reserve(size: Long): Reservation {
        if (size < 0 || size > maxBytes - bytes || files >= maxFiles) throw IOException("Receiver lifetime budget exceeded")
        bytes += size
        files++
        return Reservation(this, size)
    }

    @Synchronized private fun release(size: Long) { bytes -= size; files-- }

    class Reservation internal constructor(private val owner: FileReceiveBudget, private val size: Long) : AutoCloseable {
        private var committed = false
        private var closed = false
        @Synchronized fun commit() { check(!closed); committed = true }
        @Synchronized override fun close() {
            if (closed) return
            closed = true
            if (!committed) owner.release(size)
        }
    }
}
