package com.shterneregen.securelan.desktop.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TransferEntryTest {
    @Test
    fun shouldExposeActiveStateAndDirectionLabels() {
        val outgoing = TransferEntry("send-1", "demo.bin", true, "Sending", 0, 1024)
        val incoming = TransferEntry("recv-1", "demo.bin", false, "Receiving", 0, 1024)
        val completed = TransferEntry("done-1", "demo.bin", true, "Completed", 100, 1024)

        assertTrue(outgoing.active())
        assertEquals("↑ Sent", outgoing.directionLabel())
        assertTrue(incoming.active())
        assertEquals("↓ Received", incoming.directionLabel())
        assertFalse(completed.active())
    }

    @Test
    fun shouldUpdateProgressAndAverageSpeed() {
        val clock = SteppingClock(1_000_000_000L, 2_000_000_000L)
        val entry = TransferEntry("send-1", "demo.bin", true, "Sending", 0, 0, clock::now)

        entry.updateProgress(512, 50, 1024)

        assertEquals(50, entry.percent)
        assertEquals(1024, entry.totalBytes)
        assertEquals(512, entry.lastTransferredBytes)
        assertEquals(512.0, entry.speedBytesPerSecond)
        assertEquals(2_000_000_000L, entry.lastSpeedDisplayNanos)
    }

    @Test
    fun shouldThrottleSpeedDisplayUntilIntervalOrCompletion() {
        val clock = SteppingClock(1_000_000_000L, 2_000_000_000L, 2_100_000_000L, 2_200_000_000L)
        val entry = TransferEntry("send-1", "demo.bin", true, "Sending", 0, 0, clock::now)

        entry.updateProgress(1_000, 25, 4_000)
        val firstSpeed = entry.speedBytesPerSecond

        entry.updateProgress(2_000, 50, 4_000)
        assertEquals(firstSpeed, entry.speedBytesPerSecond)

        entry.updateProgress(4_000, 100, 4_000)
        assertEquals(4_000.0 / 1.2, entry.speedBytesPerSecond, 0.0001)
    }

    @Test
    fun shouldStopSpeedTracking() {
        val clock = SteppingClock(1_000_000_000L, 2_000_000_000L)
        val entry = TransferEntry("send-1", "demo.bin", true, "Sending", 0, 0, clock::now)
        entry.updateProgress(512, 50, 1024)

        entry.stopSpeedTracking()

        assertEquals(0.0, entry.speedBytesPerSecond)
    }

    private class SteppingClock(vararg values: Long) {
        private val values = values.toList()
        private var index = 0

        fun now(): Long = values[index.coerceAtMost(values.lastIndex)].also { index++ }
    }
}
