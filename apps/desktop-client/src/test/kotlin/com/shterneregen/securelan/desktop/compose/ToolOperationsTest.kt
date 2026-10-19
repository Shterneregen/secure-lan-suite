package com.shterneregen.securelan.desktop.compose

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

class ToolOperationsTest {
    private class ImmediateExecutor : java.util.concurrent.AbstractExecutorService() {
        private var stopped = false
        override fun execute(command: Runnable) = command.run()
        override fun shutdown() { stopped = true }
        override fun shutdownNow(): MutableList<Runnable> { shutdown(); return mutableListOf() }
        override fun isShutdown() = stopped
        override fun isTerminated() = stopped
        override fun awaitTermination(timeout: Long, unit: TimeUnit) = stopped
    }

    @Test fun startupIsPublishedBeforeAnImmediateResultAndIsNotPublishedForRejectedWork() {
        val order = mutableListOf<String>()
        ToolOperations({ it() }, ImmediateExecutor()).use { tools ->
            assertTrue(tools.execute({ order += "work"; "result" }, onStarted = { order += "started" }) {
                order += it.getOrThrow()
            })
            assertEquals(listOf("started", "work", "result"), order)
            tools.close()
            assertFalse(tools.execute({ "unexpected" }, onStarted = { fail("Closed tools must not publish startup") }) {})
        }
    }

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
                assertFalse(tools.execute({ "second" }, onStarted = { fail("Busy tools must not publish startup") }) { published = true })
                tools.invalidate()
                release.countDown()
                completions.poll(2, TimeUnit.SECONDS)!!.invoke()
                assertFalse(published)
            } finally { release.countDown() }
        }
    }
}
