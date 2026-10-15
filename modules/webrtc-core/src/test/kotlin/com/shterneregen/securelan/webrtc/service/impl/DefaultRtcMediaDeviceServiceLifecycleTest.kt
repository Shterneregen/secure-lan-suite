package com.shterneregen.securelan.webrtc.service.impl

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

class DefaultRtcMediaDeviceServiceLifecycleTest {
    @Test fun closeDoesNotWaitAndAwaitHasAnExplicitDeadline() {
        val worker = Executors.newSingleThreadExecutor()
        val gate = CountDownLatch(1)
        worker.execute { gate.await() }
        val service = DefaultRtcMediaDeviceService(worker)
        try {
            assertTrue(measureTimeMillis { service.close() } < 500)
            assertFalse(service.awaitClosed(10))
            gate.countDown()
            assertTrue(service.awaitClosed(2000))
            service.close()
        } finally { gate.countDown(); worker.shutdownNow() }
    }
}
