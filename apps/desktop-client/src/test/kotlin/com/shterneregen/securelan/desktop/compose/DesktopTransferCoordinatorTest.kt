package com.shterneregen.securelan.desktop.compose

import com.shterneregen.securelan.desktop.compose.state.transfer.DesktopBatchPhase
import com.shterneregen.securelan.desktop.compose.state.transfer.DesktopTransferAttempt
import com.shterneregen.securelan.desktop.compose.state.transfer.DesktopTransferBatch
import com.shterneregen.securelan.desktop.compose.state.transfer.DesktopTransferDraft
import com.shterneregen.securelan.desktop.compose.state.transfer.DesktopTransferPhase
import com.shterneregen.securelan.desktop.compose.state.transfer.DesktopTransferTarget
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class DesktopTransferCoordinatorTest {
    private val target = DesktopTransferTarget(generation = 1L, peerId = "peer", nickname = "Yvonne", host = "10.0.0.2", port = 5051)

    @Test
    fun shouldSendEveryFileInOrderBeforeFinishing() {
        val executed = mutableListOf<String>()
        var latest: DesktopTransferBatch? = null
        val coordinator = coordinator(
            executed = executed,
            publish = { _, batch -> latest = batch },
        )

        coordinator.add(target, listOf(Path.of("a.txt"), Path.of("b.txt"), Path.of("c.txt")))
        assertTrue(coordinator.startDraft())

        assertEquals(listOf("a.txt", "b.txt", "c.txt"), executed)
        assertNotNull(latest)
        val finished = latest!!
        assertEquals(DesktopBatchPhase.FINISHED, finished.phase)
        assertEquals(3, finished.completed)
        assertEquals(0, finished.failed)
    }

    @Test
    fun shouldPauseQueueAfterAFailedFile() {
        val executed = mutableListOf<String>()
        var latest: DesktopTransferBatch? = null
        val coordinator = coordinator(
            executed = executed,
            publish = { _, batch -> latest = batch },
            outcomeFor = { name -> if (name == "b.txt") DesktopTransferPhase.FAILED else DesktopTransferPhase.COMPLETED },
        )

        coordinator.add(target, listOf(Path.of("a.txt"), Path.of("b.txt"), Path.of("c.txt")))
        coordinator.startDraft()

        assertEquals(listOf("a.txt", "b.txt"), executed)
        assertNotNull(latest)
        val paused = latest!!
        assertEquals(DesktopBatchPhase.PAUSED, paused.phase)
        assertEquals(1, paused.completed)
        assertEquals(1, paused.failed)
        assertEquals(DesktopTransferPhase.QUEUED, paused.items.first { it.name == "c.txt" }.phase)
    }

    @Test
    fun shouldRejectNewWorkWhileABatchIsUnfinished() {
        var release: (() -> Unit)? = null
        var latest: DesktopTransferBatch? = null
        val coordinator = DesktopTransferCoordinator(
            execute = { work -> release = work },
            validTarget = { true },
            operation = { _, _, _ -> completedOperation() },
            publish = { _, batch -> latest = batch },
            inspect = { 1L },
        )

        coordinator.add(target, listOf(Path.of("a.txt")))
        coordinator.startDraft()
        // Worker is suspended because execute captured the work instead of running it.
        assertFalse(coordinator.startDraft())
        assertFalse(coordinator.add(target, listOf(Path.of("b.txt"))))
        assertEquals(DesktopBatchPhase.RUNNING, latest!!.phase)

        release?.invoke()
        assertEquals(DesktopBatchPhase.FINISHED, latest!!.phase)
    }

    @Test
    fun shouldSkipAQueuedItemRemovedWhileAnotherFileIsActive() {
        var release: (() -> Unit)? = null
        var latest: DesktopTransferBatch? = null
        val coordinator = DesktopTransferCoordinator(
            execute = { work -> release = work },
            validTarget = { true },
            operation = { _, _, _ -> completedOperation() },
            publish = { _, batch -> latest = batch },
            inspect = { 1L },
        )

        coordinator.add(target, listOf(Path.of("a.txt"), Path.of("b.txt")))
        coordinator.startDraft()
        val queued = latest!!.items.first { it.name == "b.txt" }
        coordinator.removeQueued(queued.id)

        assertEquals(DesktopTransferPhase.SKIPPED, latest!!.items.first { it.id == queued.id }.phase)
        release?.invoke()
        assertEquals(DesktopBatchPhase.FINISHED, latest!!.phase)
        assertEquals(1, latest!!.completed)
        assertEquals(1, latest!!.skipped)
    }

    @Test
    fun shouldRefuseDraftForInvalidTarget() {
        val coordinator = DesktopTransferCoordinator(
            execute = { it() },
            validTarget = { false },
            operation = { _, _, _ -> completedOperation() },
            publish = { _, _ -> },
            inspect = { 1L },
        )

        assertFalse(coordinator.add(target, listOf(Path.of("a.txt"))))
        assertFalse(coordinator.startDraft())
    }

    @Test
    fun shouldRetainAnUnconfirmedAttemptWhenRetryingAndIgnoreItsLateProgress() {
        val callbacks = mutableListOf<(DesktopTransferAttempt) -> Unit>()
        var latest: DesktopTransferBatch? = null
        val coordinator = DesktopTransferCoordinator(
            execute = { it() },
            validTarget = { true },
            operation = { _, _, update ->
                callbacks += update
                val phase = if (callbacks.size == 1) DesktopTransferPhase.OUTCOME_UNKNOWN else DesktopTransferPhase.COMPLETED
                object : DesktopTransferOperation {
                    override fun execute() = DesktopTransferAttempt("", phase = phase)
                    override fun close() = Unit
                }
            },
            publish = { _, batch -> latest = batch },
            inspect = { 1L },
        )

        coordinator.add(target, listOf(Path.of("a.txt")))
        coordinator.startDraft()
        assertEquals(DesktopBatchPhase.PAUSED, latest?.phase)
        assertTrue(coordinator.retry())
        val finished = latest
        callbacks.first()(DesktopTransferAttempt("", phase = DesktopTransferPhase.SENDING))

        assertEquals(finished, latest)
        assertEquals(DesktopBatchPhase.FINISHED, latest?.phase)
        assertEquals(
            listOf(DesktopTransferPhase.OUTCOME_UNKNOWN, DesktopTransferPhase.COMPLETED),
            latest?.items?.single()?.attempts?.map { it.phase },
        )
    }

    @Test
    fun shouldFinishTheCurrentFileAndSkipRemainingFilesWhenStopped() {
        var release: (() -> Unit)? = null
        var latest: DesktopTransferBatch? = null
        val coordinator = DesktopTransferCoordinator(
            execute = { release = it },
            validTarget = { true },
            operation = { _, _, _ -> completedOperation() },
            publish = { _, batch -> latest = batch },
            inspect = { 1L },
        )

        coordinator.add(target, listOf(Path.of("a.txt"), Path.of("b.txt")))
        coordinator.startDraft()
        coordinator.stopAfterCurrent()
        release?.invoke()

        assertEquals(DesktopBatchPhase.FINISHED, latest?.phase)
        assertEquals(1, latest?.completed)
        assertEquals(1, latest?.skipped)
    }

    @Test
    fun sessionInvalidationShouldNotThrowWhenClosingTheActiveOperationFails() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        var worker: CompletableFuture<Void>? = null
        var latest: DesktopTransferBatch? = null
        val coordinator = DesktopTransferCoordinator(
            execute = { work -> worker = CompletableFuture.runAsync { work() } },
            validTarget = { true },
            operation = { _, _, _ ->
                object : DesktopTransferOperation {
                    override fun execute(): DesktopTransferAttempt {
                        entered.countDown()
                        check(release.await(2, TimeUnit.SECONDS))
                        return DesktopTransferAttempt("", phase = DesktopTransferPhase.COMPLETED)
                    }
                    override fun close() { error("Simulated close failure") }
                }
            },
            publish = { _, batch -> latest = batch },
            inspect = { 1L },
        )

        try {
            coordinator.add(target, listOf(Path.of("a.txt"), Path.of("b.txt")))
            coordinator.startDraft()
            assertTrue(entered.await(2, TimeUnit.SECONDS))
            assertDoesNotThrow { coordinator.invalidateSession() }
        } finally {
            release.countDown()
            worker?.get(2, TimeUnit.SECONDS)
        }
        assertEquals(DesktopBatchPhase.FINISHED, latest?.phase)
        assertEquals(1, latest?.skipped)
    }

    private fun coordinator(
        executed: MutableList<String>,
        publish: (DesktopTransferDraft?, DesktopTransferBatch?) -> Unit,
        outcomeFor: (String) -> DesktopTransferPhase = { DesktopTransferPhase.COMPLETED },
    ) = DesktopTransferCoordinator(
        execute = { work -> work() },
        validTarget = { true },
        operation = { _, item, _ ->
            executed += item.name
            object : DesktopTransferOperation {
                override fun execute(): DesktopTransferAttempt =
                    DesktopTransferAttempt("", phase = outcomeFor(item.name))
                override fun close() = Unit
            }
        },
        publish = publish,
        inspect = { 1L },
    )

    private fun completedOperation() = object : DesktopTransferOperation {
        override fun execute(): DesktopTransferAttempt = DesktopTransferAttempt("", phase = DesktopTransferPhase.COMPLETED)
        override fun close() = Unit
    }
}
