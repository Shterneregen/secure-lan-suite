package com.shterneregen.securelan.filetransfer

import com.shterneregen.securelan.filetransfer.protocol.FileTransferStreams
import com.shterneregen.securelan.filetransfer.protocol.TransferSourceException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.CancellationException

class FileTransferStreamsTest {
    @Test fun zeroAndNonemptyStreamsHaveExactLengthAndRequireAnEof() {
        for (bytes in listOf(byteArrayOf(), ByteArray(40_000) { it.toByte() })) {
            val chunks = ArrayDeque<ByteArray>()
            assertEquals(bytes.size.toLong(), FileTransferStreams.send(ByteArrayInputStream(bytes), bytes.size.toLong(), { chunks.addLast(it) }))
            chunks.addLast(byteArrayOf())
            val output = ByteArrayOutputStream()
            assertEquals(bytes.size.toLong(), FileTransferStreams.receive(output, bytes.size.toLong(), { chunks.removeFirst() }))
            assertArrayEquals(bytes, output.toByteArray())
        }
    }
    @Test fun changedSourceSizeAndEarlyOrExcessReceiveFailClosed() {
        assertThrows(TransferSourceException::class.java) { FileTransferStreams.send(ByteArrayInputStream(byteArrayOf(1)), 0, {}) }
        assertThrows(TransferSourceException::class.java) { FileTransferStreams.send(ByteArrayInputStream(byteArrayOf()), 1, {}) }
        assertThrows(IOException::class.java) { FileTransferStreams.receive(ByteArrayOutputStream(), 1, { byteArrayOf() }) }
        assertThrows(IOException::class.java) { FileTransferStreams.receive(ByteArrayOutputStream(), 0, { byteArrayOf(1) }) }
        assertThrows(CancellationException::class.java) {
            FileTransferStreams.send(ByteArrayInputStream(byteArrayOf(1)), 1, {}, { throw CancellationException() })
        }
    }
}
