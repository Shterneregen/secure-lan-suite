package com.shterneregen.securelan.androidclient.transfer

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransferCoordinatorTest {
    private val recipient = TransferRecipient(id = "peer", name = "Peer", host = "10.0.0.2", port = 9000, generation = 1L)

    private fun source(reference: String, size: Long? = 1L) = TransferSource(reference = reference, name = reference, size = size)

    @Test
    fun `draft deduplicates by source reference and preserves order`() = runTest {
        val coordinator = TransferCoordinator(this, { true }, { _, _, _ -> })
        assertTrue(coordinator.beginSelection(recipient))
        val token = requireNotNull(coordinator.selectionToken())
        coordinator.addSelection(token, listOf(source("a"), source("a"), source("b")))

        val items = coordinator.state.value.items
        assertEquals(listOf("a", "b"), items.map { it.source.reference })
    }

    @Test
    fun `start is ignored while a batch is already running`() = runTest {
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        val coordinator = TransferCoordinator(this, { true }, { _, _, _ -> calls++; gate.await() })
        assertTrue(coordinator.beginSelection(recipient))
        coordinator.addSelection(requireNotNull(coordinator.selectionToken()), listOf(source("a")))

        coordinator.start()
        advanceUntilIdle()
        coordinator.start()
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, calls)
        assertEquals(BatchStatus.FINISHED, coordinator.state.value.status)
    }

    @Test
    fun `worker sends items in FIFO order`() = runTest {
        val sent = mutableListOf<String>()
        val coordinator = TransferCoordinator(this, { true }, { _, transferSource, onUpdate ->
            sent += transferSource.reference
            onUpdate(TransferAttempt(phase = TransferPhase.SENDING, bytes = 1, total = 1))
            onUpdate(TransferAttempt(phase = TransferPhase.COMPLETED, bytes = 1, total = 1))
        })
        assertTrue(coordinator.beginSelection(recipient))
        coordinator.addSelection(requireNotNull(coordinator.selectionToken()), listOf(source("first"), source("second"), source("third")))

        coordinator.start()
        advanceUntilIdle()

        assertEquals(listOf("first", "second", "third"), sent)
        assertEquals(BatchStatus.FINISHED, coordinator.state.value.status)
        assertEquals(3, coordinator.state.value.confirmed)
    }

    @Test
    fun `failure pauses the batch and records the failed item`() = runTest {
        var calls = 0
        val coordinator = TransferCoordinator(this, { true }, { _, _, _ ->
            calls++
            throw IllegalStateException("provider unavailable")
        })
        assertTrue(coordinator.beginSelection(recipient))
        coordinator.addSelection(requireNotNull(coordinator.selectionToken()), listOf(source("a"), source("b")))

        coordinator.start()
        advanceUntilIdle()

        assertEquals(1, calls)
        assertEquals(BatchStatus.PAUSED, coordinator.state.value.status)
        assertEquals(1, coordinator.state.value.failed)
        assertEquals(1, coordinator.state.value.items.count { it.phase == TransferPhase.QUEUED })
    }

    @Test
    fun `lost confirmation after EOF is recorded as outcome unknown`() = runTest {
        val coordinator = TransferCoordinator(this, { true }, { _, _, onUpdate ->
            onUpdate(TransferAttempt(phase = TransferPhase.WAITING_FOR_CONFIRMATION, bytes = 1, total = 1))
            throw IllegalStateException("confirmation unavailable")
        })
        assertTrue(coordinator.beginSelection(recipient))
        coordinator.addSelection(requireNotNull(coordinator.selectionToken()), listOf(source("a")))

        coordinator.start()
        advanceUntilIdle()

        assertEquals(BatchStatus.PAUSED, coordinator.state.value.status)
        assertEquals(1, coordinator.state.value.items.count { it.phase == TransferPhase.OUTCOME_UNKNOWN })
    }

    @Test
    fun `retry requeues failed items and completes them`() = runTest {
        var fail = true
        val coordinator = TransferCoordinator(this, { true }, { _, _, _ ->
            if (fail) throw IllegalStateException("temporary")
        })
        assertTrue(coordinator.beginSelection(recipient))
        coordinator.addSelection(requireNotNull(coordinator.selectionToken()), listOf(source("a")))

        coordinator.start()
        advanceUntilIdle()
        assertEquals(BatchStatus.PAUSED, coordinator.state.value.status)

        fail = false
        coordinator.retry()
        advanceUntilIdle()

        assertEquals(BatchStatus.FINISHED, coordinator.state.value.status)
        assertEquals(1, coordinator.state.value.confirmed)
        assertEquals(0, coordinator.state.value.failed)
    }

    @Test
    fun `skip and continue resumes after a failure without rewriting it`() = runTest {
        var calls = 0
        val coordinator = TransferCoordinator(this, { true }, { _, _, _ ->
            calls++
            if (calls == 1) throw IllegalStateException("first fails")
        })
        assertTrue(coordinator.beginSelection(recipient))
        coordinator.addSelection(requireNotNull(coordinator.selectionToken()), listOf(source("a"), source("b")))

        coordinator.start()
        advanceUntilIdle()
        assertEquals(BatchStatus.PAUSED, coordinator.state.value.status)

        coordinator.skipAndContinue()
        advanceUntilIdle()

        assertEquals(BatchStatus.FINISHED, coordinator.state.value.status)
        assertEquals(1, coordinator.state.value.failed)
        assertEquals(1, coordinator.state.value.confirmed)
    }

    @Test
    fun `stop after current skips remaining queued items`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val sent = mutableListOf<String>()
        val coordinator = TransferCoordinator(this, { true }, { _, transferSource, _ ->
            sent += transferSource.reference
            gate.await()
        })
        assertTrue(coordinator.beginSelection(recipient))
        coordinator.addSelection(requireNotNull(coordinator.selectionToken()), listOf(source("a"), source("b")))

        coordinator.start()
        advanceUntilIdle()
        coordinator.stopAfterCurrent()
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf("a"), sent)
        assertEquals(BatchStatus.FINISHED, coordinator.state.value.status)
        assertEquals(1, coordinator.state.value.skipped)
    }

    @Test
    fun `remove marks a queued item skipped while another item is active`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val coordinator = TransferCoordinator(this, { true }, { _, _, _ -> gate.await() })
        assertTrue(coordinator.beginSelection(recipient))
        coordinator.addSelection(requireNotNull(coordinator.selectionToken()), listOf(source("a"), source("b")))

        coordinator.start()
        advanceUntilIdle()
        val queued = coordinator.state.value.items.first { it.source.reference == "b" }
        coordinator.remove(queued.id)
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(BatchStatus.FINISHED, coordinator.state.value.status)
        assertEquals(1, coordinator.state.value.confirmed)
        assertEquals(1, coordinator.state.value.skipped)
    }

    @Test
    fun `invalidated recipient stops the queue without redirecting it`() = runTest {
        var available = true
        var calls = 0
        val coordinator = TransferCoordinator(this, { available }, { _, _, _ -> calls++ })
        assertTrue(coordinator.beginSelection(recipient))
        coordinator.addSelection(requireNotNull(coordinator.selectionToken()), listOf(source("a")))

        available = false
        coordinator.start()
        advanceUntilIdle()

        assertEquals(0, calls)
        assertEquals(BatchStatus.PAUSED, coordinator.state.value.status)
    }

    @Test
    fun `beginSelection rejects an unavailable recipient`() = runTest {
        val coordinator = TransferCoordinator(this, { false }, { _, _, _ -> })
        assertFalse(coordinator.beginSelection(recipient))
    }

    @Test
    fun `knownBytes sums sizes and is null when any size is unknown`() {
        val known = TransferQueueState(items = listOf(TransferItem(source = source("a", 5L)), TransferItem(source = source("b", 7L))))
        assertEquals(12L, known.knownBytes)

        val unknown = TransferQueueState(items = listOf(TransferItem(source = source("a", 5L)), TransferItem(source = source("b", null))))
        assertNull(unknown.knownBytes)
    }

    @Test
    fun `remove drops a queued item from a ready draft`() = runTest {
        val coordinator = TransferCoordinator(this, { true }, { _, _, _ -> })
        assertTrue(coordinator.beginSelection(recipient))
        coordinator.addSelection(requireNotNull(coordinator.selectionToken()), listOf(source("a"), source("b")))

        val id = coordinator.state.value.items.first().id
        coordinator.remove(id)

        assertEquals(listOf("b"), coordinator.state.value.items.map { it.source.reference })
    }
}
