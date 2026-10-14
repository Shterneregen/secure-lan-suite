package com.shterneregen.securelan.webrtc.runtime

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SharedMediaDeviceTest {
    @Test fun captureStopsAfterLastSessionAndCanRestartForAnotherAcceptedCall() {
        var starts = 0
        var stops = 0
        SharedMediaDevice({ starts++ }, { stops++ }).use { device ->
            val first = device.acquire()
            val second = device.acquire()
            assertEquals(1, starts)
            first.close()
            assertEquals(0, stops)
            second.close()
            second.close()
            assertEquals(1, stops)
            device.acquire().close()
            assertEquals(2, starts)
            assertEquals(2, stops)
        }
    }

    @Test fun shutdownReleasesOutstandingLeasesAndRejectsFurtherCapture() {
        var stops = 0
        val device = SharedMediaDevice({}, { stops++ })
        val lease = device.acquire()
        device.close()
        lease.close()
        device.close()
        assertEquals(1, stops)
        assertThrows(IllegalStateException::class.java) { device.acquire() }
    }

    @Test fun failedStartDoesNotLeaveAnOwnedDevice() {
        var stops = 0
        SharedMediaDevice({ throw IllegalStateException("unavailable") }, { stops++ }).use { device ->
            assertThrows(IllegalStateException::class.java) { device.acquire() }
        }
        assertEquals(0, stops)
    }
}
