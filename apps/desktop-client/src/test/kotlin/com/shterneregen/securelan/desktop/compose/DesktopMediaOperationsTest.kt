package com.shterneregen.securelan.desktop.compose

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class DesktopMediaOperationsTest {
    @Test fun blockedDriverDoesNotBlockCallerAndLateResultCannotReplaceTimeout() {
        val ui = LinkedBlockingQueue<() -> Unit>()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val finished = CountDownLatch(1)
        val results = mutableListOf<Result<String>>()
        DesktopMediaOperations({ ui.add(it) }, 100).use { operations ->
            try {
                operations.submit("camera", {
                    entered.countDown()
                    check(release.await(3, TimeUnit.SECONDS))
                    finished.countDown()
                    "late success"
                }, results::add)
                assertTrue(entered.await(2, TimeUnit.SECONDS))
                ui.poll(2, TimeUnit.SECONDS)!!.invoke()
                assertEquals(1, results.size)
                assertTrue(results.single().isFailure)
                release.countDown()
                assertTrue(finished.await(2, TimeUnit.SECONDS))
                operations.close()
                while (true) (ui.poll() ?: break).invoke()
                assertEquals(1, results.size)
            } finally { release.countDown() }
        }
    }

    @Test fun invalidationDiscardsAlreadyQueuedUiResult() {
        val ui = LinkedBlockingQueue<() -> Unit>()
        var published = false
        DesktopMediaOperations({ ui.add(it) }).use { operations ->
            operations.submit("microphone", { "old device" }) { published = true }
            val task = ui.poll(2, TimeUnit.SECONDS)!!
            operations.invalidate("microphone")
            task()
            assertFalse(published)
        }
    }
}
