package com.shterneregen.securelan.desktop.compose

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

class ToolOperationsTest {
    @Test fun blockedToolDoesNotBlockTheCallerAndDismissedResultsAreIgnored() {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val completions = LinkedBlockingQueue<() -> Unit>()
        var published = false
        ToolOperations({ completions.add(it) }).use { tools ->
            try {
                assertTrue(measureTimeMillis {
                    assertTrue(tools.execute({ started.countDown(); release.await(); "result" }) { published = true })
                } < 500)
                assertTrue(started.await(2, TimeUnit.SECONDS))
                assertFalse(tools.execute({ "second" }) { published = true })
                tools.invalidate()
                release.countDown()
                completions.poll(2, TimeUnit.SECONDS)!!.invoke()
                assertFalse(published)
            } finally { release.countDown() }
        }
    }
}
