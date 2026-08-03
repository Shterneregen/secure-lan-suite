package com.shterneregen.securelan.filetransfer.service.impl

import com.shterneregen.securelan.filetransfer.event.FileTransferProgressEvent
import com.shterneregen.securelan.filetransfer.service.FileTransferEventPublisher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ThrottledFileTransferProgressPublisherTest {
    @Test
    fun shouldThrottleIntermediateUpdatesAndAlwaysPublishCompletion() {
        var now = 1_000L
        val events = mutableListOf<FileTransferProgressEvent>()
        val publisher = ThrottledFileTransferProgressPublisher(
            FileTransferEventPublisher { event -> events += event as FileTransferProgressEvent },
            transferId = "transfer-1",
            totalBytes = 1_000L,
            outgoing = false,
            minimumIntervalNanos = 100L,
            nanoTime = { now },
        )

        publisher.report(10L)
        now += 99L
        publisher.report(500L)
        now += 1L
        publisher.report(600L)
        publisher.report(1_000L)

        assertEquals(listOf(10L, 600L, 1_000L), events.map { it.progress!!.transferredBytes })
        assertFalse(events.first().outgoing)
        assertEquals(100, events.last().progress!!.percent())
    }

    @Test
    fun shouldPublishAtMostOneUpdatePerIntervalDuringChunkBurst() {
        var now = 5_000L
        val events = mutableListOf<FileTransferProgressEvent>()
        val publisher = ThrottledFileTransferProgressPublisher(
            FileTransferEventPublisher { event -> events += event as FileTransferProgressEvent },
            transferId = "transfer-2",
            totalBytes = 10_000L,
            outgoing = true,
            minimumIntervalNanos = 100L,
            nanoTime = { now },
        )

        for (transferred in 1L..9_999L) {
            publisher.report(transferred)
        }
        publisher.report(10_000L)

        assertEquals(2, events.size)
        assertEquals(1L, events.first().progress!!.transferredBytes)
        assertEquals(10_000L, events.last().progress!!.transferredBytes)
        assertTrue(events.all { it.outgoing })
    }
}
