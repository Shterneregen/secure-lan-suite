package com.shterneregen.securelan.androidclient.transfer

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class TransferPhase { QUEUED, PREPARING, WAITING_FOR_ACCEPTANCE, SENDING, WAITING_FOR_CONFIRMATION, COMPLETED, FAILED, OUTCOME_UNKNOWN, SKIPPED }
enum class BatchStatus { READY, RUNNING, STOP_REQUESTED, PAUSED, FINISHED }
data class TransferRecipient(val id: String, val name: String, val host: String, val port: Int, val generation: Long)
data class TransferSource(val reference: String, val name: String, val size: Long?, val unavailable: Boolean = false)
data class TransferAttempt(val id: String = UUID.randomUUID().toString(), val wireId: String? = null, val phase: TransferPhase = TransferPhase.PREPARING, val bytes: Long = 0, val total: Long? = null)
data class TransferItem(val id: String = UUID.randomUUID().toString(), val source: TransferSource, val phase: TransferPhase = TransferPhase.QUEUED, val attempts: List<TransferAttempt> = emptyList())
data class TransferQueueState(
    val recipient: TransferRecipient? = null,
    val items: List<TransferItem> = emptyList(),
    val status: BatchStatus = BatchStatus.READY,
    val loading: Boolean = false,
) {
    val confirmed get() = items.count { it.phase == TransferPhase.COMPLETED }
    val failed get() = items.count { it.phase == TransferPhase.FAILED || it.phase == TransferPhase.OUTCOME_UNKNOWN }
    val skipped get() = items.count { it.phase == TransferPhase.SKIPPED }
    val active get() = status == BatchStatus.RUNNING || status == BatchStatus.STOP_REQUESTED
    val knownBytes: Long? get() {
        var sum = 0L
        for (item in items) {
            val size = item.source.size ?: return null
            if (size > Long.MAX_VALUE - sum) return null
            sum += size
        }
        return sum
    }
}

/** One application-owned slot, including paused work. Callbacks are attempt-correlated. */
class TransferCoordinator(
    private val scope: CoroutineScope,
    private val isAvailable: (TransferRecipient) -> Boolean,
    private val send: suspend (TransferRecipient, TransferSource, (TransferAttempt) -> Unit) -> Unit,
) {
    private val mutableState = MutableStateFlow(TransferQueueState())
    val state = mutableState.asStateFlow()
    private var worker = false
    private var picker: TransferRecipient? = null
    private var revision = 0L

    @Synchronized fun beginSelection(recipient: TransferRecipient): Boolean {
        val s = mutableState.value
        if (worker || s.loading || s.status == BatchStatus.PAUSED || !isAvailable(recipient)) return false
        if (s.status == BatchStatus.READY && s.items.isNotEmpty() && s.recipient != recipient) return false
        picker = recipient
        return true
    }

    @Synchronized fun selectionToken(): Pair<TransferRecipient, Long>? {
        val recipient = picker ?: return null
        picker = null
        if (!isAvailable(recipient)) return null
        mutableState.value = mutableState.value.copy(loading = true)
        return recipient to revision
    }

    @Synchronized fun addSelection(token: Pair<TransferRecipient, Long>, sources: List<TransferSource>) {
        val s = mutableState.value
        if (token.second != revision) return
        mutableState.value = s.copy(loading = false)
        if (sources.isEmpty() || worker || s.status == BatchStatus.PAUSED || !isAvailable(token.first)) return
        val previous = if (s.status == BatchStatus.READY) s.items else emptyList()
        mutableState.value = TransferQueueState(token.first, (previous + sources.map { TransferItem(source = it) }).distinctBy { it.source.reference })
    }

    @Synchronized fun clear() {
        if (worker || mutableState.value.status == BatchStatus.PAUSED) return
        revision++
        picker = null
        mutableState.value = TransferQueueState()
    }

    @Synchronized fun remove(id: String) {
        val s = mutableState.value
        if (s.status == BatchStatus.READY) {
            mutableState.value = s.copy(items = s.items.filterNot { it.id == id })
        } else if (s.status != BatchStatus.FINISHED) {
            mutableState.value = s.copy(items = s.items.map { if (it.id == id && it.phase == TransferPhase.QUEUED) it.copy(phase = TransferPhase.SKIPPED) else it })
        }
    }

    @Synchronized fun start() {
        val s = mutableState.value
        if (worker || s.loading || s.status != BatchStatus.READY || s.items.isEmpty()) return
        launchWorker()
    }

    @Synchronized fun retry() {
        val s = mutableState.value
        if (worker || s.status != BatchStatus.PAUSED || s.recipient?.let(isAvailable) != true) return
        mutableState.value = s.copy(items = s.items.map { if (it.phase == TransferPhase.FAILED || it.phase == TransferPhase.OUTCOME_UNKNOWN) it.copy(phase = TransferPhase.QUEUED) else it })
        launchWorker()
    }

    @Synchronized fun skipAndContinue() {
        val s = mutableState.value
        if (worker || s.status != BatchStatus.PAUSED || s.recipient?.let(isAvailable) != true) return
        // Failed attempts retain their outcome; continuing never rewrites history.
        launchWorker()
    }

    @Synchronized fun stopAfterCurrent() {
        val s = mutableState.value
        if (worker) mutableState.value = s.copy(status = BatchStatus.STOP_REQUESTED)
        else if (s.status == BatchStatus.PAUSED) finish()
    }

    @Synchronized private fun finish() {
        mutableState.value = mutableState.value.copy(status = BatchStatus.FINISHED, items = mutableState.value.items.map {
            if (it.phase == TransferPhase.QUEUED) it.copy(phase = TransferPhase.SKIPPED) else it
        })
    }

    private fun launchWorker() {
        worker = true
        mutableState.value = mutableState.value.copy(status = BatchStatus.RUNNING)
        scope.launch {
            try {
                while (true) {
                    val work = synchronized(this@TransferCoordinator) {
                        val s = mutableState.value
                        if (s.status == BatchStatus.STOP_REQUESTED) { finish(); return@launch }
                        val item = s.items.firstOrNull { it.phase == TransferPhase.QUEUED }
                        if (item == null) { finish(); return@launch }
                        val recipient = requireNotNull(s.recipient)
                        val attempt = TransferAttempt(total = item.source.size)
                        mutableState.value = s.copy(items = s.items.map { if (it.id == item.id) it.copy(phase = TransferPhase.PREPARING, attempts = it.attempts + attempt) else it })
                        Triple(recipient, item, attempt)
                    }
                    val (recipient, item, attempt) = work
                    var outcome = TransferPhase.COMPLETED
                    var cancellation: CancellationException? = null
                    try {
                        check(isAvailable(recipient))
                        send(recipient, item.source) { update -> updateAttempt(item.id, attempt.id, update.copy(id = attempt.id)) }
                    } catch (error: Exception) {
                        val phase = mutableState.value.items.first { it.id == item.id }.attempts.last().phase
                        outcome = if (phase == TransferPhase.WAITING_FOR_CONFIRMATION) TransferPhase.OUTCOME_UNKNOWN else TransferPhase.FAILED
                        if (error is CancellationException) cancellation = error
                    }
                    synchronized(this@TransferCoordinator) {
                        val current = mutableState.value.items.first { it.id == item.id }.attempts.last()
                        updateAttempt(item.id, attempt.id, current.copy(phase = outcome))
                        if (mutableState.value.status == BatchStatus.STOP_REQUESTED) finish()
                        else if (outcome != TransferPhase.COMPLETED) mutableState.value = mutableState.value.copy(status = BatchStatus.PAUSED)
                    }
                    cancellation?.let { throw it }
                    if (mutableState.value.status != BatchStatus.RUNNING) return@launch
                }
            } finally {
                synchronized(this@TransferCoordinator) { worker = false }
            }
        }
    }

    @Synchronized private fun updateAttempt(itemId: String, attemptId: String, update: TransferAttempt) {
        mutableState.value = mutableState.value.copy(items = mutableState.value.items.map { item ->
            val last = item.attempts.lastOrNull()
            if (item.id != itemId || last?.id != attemptId || last.phase in TERMINAL) item
            else item.copy(phase = update.phase, attempts = item.attempts.dropLast(1) + update)
        })
    }

    private companion object {
        val TERMINAL = setOf(TransferPhase.COMPLETED, TransferPhase.FAILED, TransferPhase.OUTCOME_UNKNOWN, TransferPhase.SKIPPED)
    }
}
