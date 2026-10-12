package com.shterneregen.securelan.desktop.compose

import com.shterneregen.securelan.desktop.compose.state.transfer.DesktopBatchPhase
import com.shterneregen.securelan.desktop.compose.state.transfer.DesktopTransferAttempt
import com.shterneregen.securelan.desktop.compose.state.transfer.DesktopTransferBatch
import com.shterneregen.securelan.desktop.compose.state.transfer.DesktopTransferDraft
import com.shterneregen.securelan.desktop.compose.state.transfer.DesktopTransferItem
import com.shterneregen.securelan.desktop.compose.state.transfer.DesktopTransferPhase
import com.shterneregen.securelan.desktop.compose.state.transfer.DesktopTransferTarget
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.CompletableFuture

/** One blocking file attempt whose resources are released before its outcome is published. */
internal interface DesktopTransferOperation : AutoCloseable {
    /** Returns a terminal result; local byte progress alone must not indicate successful delivery. */
    fun execute(): DesktopTransferAttempt
}

/**
 * Owns one recipient-bound draft and one unfinished outgoing batch, including paused attempts.
 *
 * Public actions and progress callbacks serialize access through this instance's monitor.
 * [publish] runs under that monitor, sometimes on a worker thread, and must not block on the UI.
 * [execute] is supplied by the host adapter to schedule blocking network work away from the UI.
 * A failed or unconfirmed file pauses the queue until the user retries, skips, or finishes it.
 */
internal class DesktopTransferCoordinator(
    private val execute: (() -> Unit) -> Unit,
    private val validTarget: (DesktopTransferTarget) -> Boolean,
    private val operation: (DesktopTransferTarget, DesktopTransferItem, (DesktopTransferAttempt) -> Unit) -> DesktopTransferOperation,
    private val publish: (DesktopTransferDraft?, DesktopTransferBatch?) -> Unit,
    private val inspect: (Path) -> Long? = { if (Files.isRegularFile(it) && Files.isReadable(it)) Files.size(it) else null },
) {
    private var draft: DesktopTransferDraft? = null
    private var batch: DesktopTransferBatch? = null
    private var active: DesktopTransferOperation? = null
    private var worker = false
    private var invalidated = false
    private var completion: CompletableFuture<String?>? = null
    private var pausedItemId: String? = null

    /** Adds normalized, distinct paths while preserving the draft's original recipient. */
    @Synchronized
    fun add(target: DesktopTransferTarget, paths: List<Path>): Boolean {
        if (paths.isEmpty()) return false
        if (batch?.unfinished == true || !validTarget(target)) return false
        if (draft?.target?.let { it != target } == true) return false
        val existing = draft?.items.orEmpty()
        val seen = existing.mapTo(HashSet()) { it.path }
        val added = paths.map { it.toAbsolutePath().normalize() }.filter { seen.add(it) }
            .map { DesktopTransferItem(UUID.randomUUID().toString(), it, runCatching { inspect(it) }.getOrNull()) }
        draft = DesktopTransferDraft(target, existing + added)
        emit()
        return true
    }

    /** Removes a file from the review draft without affecting previously sent files. */
    @Synchronized
    fun removeDraft(id: String) {
        draft = draft?.let { current -> current.copy(items = current.items.filterNot { it.id == id }) }
        emit()
    }

    /** Discards only the unsent review draft. */
    @Synchronized
    fun clearDraft() {
        draft = null
        emit()
    }

    /** Starts the reviewed files if their recipient still belongs to the current connection. */
    @Synchronized
    fun startDraft(): Boolean {
        val ready = draft ?: return false
        return start(ready.target, ready.items)
    }

    /** Returns the confirmed wire transfer ID, or `null` when the attempt cannot succeed. */
    @Synchronized
    fun startSingle(target: DesktopTransferTarget, path: Path): CompletableFuture<String?> {
        val result = CompletableFuture<String?>()
        val item = DesktopTransferItem(UUID.randomUUID().toString(), path.toAbsolutePath().normalize())
        if (!start(target, listOf(item), result)) result.complete(null)
        return result
    }

    private fun start(target: DesktopTransferTarget, items: List<DesktopTransferItem>, result: CompletableFuture<String?>? = null): Boolean {
        if (worker || batch?.unfinished == true || items.isEmpty() || !validTarget(target)) return false
        batch = DesktopTransferBatch(UUID.randomUUID().toString(), target, items.toList())
        draft = null
        invalidated = false
        completion = result
        advance()
        return true
    }

    /** Explicitly retries the paused file; an unconfirmed earlier attempt may already be saved. */
    @Synchronized
    fun retry(): Boolean {
        val current = batch ?: return false
        if (worker || current.phase != DesktopBatchPhase.PAUSED || invalidated || !validTarget(current.target)) return false
        val failed = current.items.firstOrNull { it.id == pausedItemId && it.phase in failurePhases } ?: return false
        replace(failed.copy(phase = DesktopTransferPhase.QUEUED))
        batch = batch?.copy(phase = DesktopBatchPhase.RUNNING)
        advance()
        return true
    }

    /** Preserves the failed attempt and advances to the next queued file. */
    @Synchronized
    fun skipAndContinue() {
        val current = batch ?: return
        if (worker || current.phase != DesktopBatchPhase.PAUSED || invalidated || !validTarget(current.target)) return
        // Keep the failed attempt and its outcome; only its remaining work is skipped.
        batch = current.copy(phase = DesktopBatchPhase.RUNNING)
        advance()
    }

    /** Leaves an active attempt running and skips the queue once it returns. */
    @Synchronized
    fun stopAfterCurrent() {
        val current = batch ?: return
        if (!current.unfinished) return
        batch = current.copy(phase = DesktopBatchPhase.STOP_REQUESTED)
        if (!worker) finish() else emit()
    }

    /** Marks a queued file as skipped while retaining it in the batch summary. */
    @Synchronized
    fun removeQueued(id: String) {
        val current = batch ?: return
        if (!current.unfinished) return
        val item = current.items.firstOrNull { it.id == id && it.phase == DesktopTransferPhase.QUEUED } ?: return
        replace(item.copy(phase = DesktopTransferPhase.SKIPPED))
        emit()
    }

    /** Stops work for the old connection so a batch cannot follow the user into another room. */
    @Synchronized
    fun invalidateSession() {
        invalidated = true
        val current = batch
        if (current?.unfinished == true) {
            batch = current.copy(phase = DesktopBatchPhase.STOP_REQUESTED)
            try {
                active?.close()
            } catch (_: Exception) {
                // Continue the host adapter's remaining shutdown steps after a close failure.
            }
            if (!worker) finish() else emit()
        }
    }

    private fun advance() {
        val current = batch ?: return
        if (worker) return
        if (current.phase == DesktopBatchPhase.STOP_REQUESTED || invalidated) {
            finish()
            return
        }
        if (!validTarget(current.target)) {
            invalidated = true
            finish()
            return
        }
        val item = current.items.firstOrNull { it.phase == DesktopTransferPhase.QUEUED }
        if (item == null) {
            finish()
            return
        }
        val attemptId = UUID.randomUUID().toString()
        replace(item.copy(phase = DesktopTransferPhase.PREPARING, attempts = item.attempts + DesktopTransferAttempt(attemptId)))
        worker = true
        emit()
        execute {
            val outcome = try {
                val size = inspect(item.path) ?: error("Source unavailable")
                val op = synchronized(this) {
                    if (invalidated || !validTarget(current.target)) error("Connection changed")
                    operation(current.target, item.copy(size = size)) { snapshot -> update(current.id, item.id, attemptId, snapshot) }
                        .also { active = it }
                }
                op.use { it.execute().copy(id = attemptId, size = size) }
            } catch (_: Exception) {
                DesktopTransferAttempt(attemptId, phase = DesktopTransferPhase.FAILED, error = "File unavailable or connection changed. Check the file and recipient.")
            }
            synchronized(this) {
                active = null
                worker = false
                val latest = batch?.items?.firstOrNull { it.id == item.id } ?: return@synchronized
                replace(latest.copy(phase = outcome.phase, size = outcome.size ?: latest.size, attempts = latest.attempts.dropLast(1) + outcome))
                completion?.complete(outcome.wireId.takeIf { outcome.phase == DesktopTransferPhase.COMPLETED })
                if (batch?.phase == DesktopBatchPhase.STOP_REQUESTED || invalidated) finish()
                else if (outcome.phase in failurePhases) {
                    pausedItemId = item.id
                    batch = batch?.copy(phase = DesktopBatchPhase.PAUSED)
                    emit()
                } else advance()
            }
        }
    }

    @Synchronized
    private fun update(batchId: String, itemId: String, attemptId: String, update: DesktopTransferAttempt) {
        val current = batch ?: return
        if (invalidated || current.id != batchId || !worker) return
        val item = current.items.firstOrNull { it.id == itemId } ?: return
        if (item.attempts.lastOrNull()?.id != attemptId || item.phase in terminalPhases) return
        // Completion belongs to execute(), after the operation has released its resources.
        if (update.phase in terminalPhases) return
        replace(item.copy(phase = update.phase, size = update.size ?: item.size, attempts = item.attempts.dropLast(1) + update.copy(id = attemptId)))
        emit()
    }

    private fun replace(item: DesktopTransferItem) {
        val current = checkNotNull(batch) { "An item update requires an active batch" }
        batch = current.copy(items = current.items.map { if (it.id == item.id) item else it })
    }

    private fun finish() {
        batch = batch?.let { current ->
            current.copy(
                phase = DesktopBatchPhase.FINISHED,
                items = current.items.map { item ->
                    if (item.phase == DesktopTransferPhase.QUEUED) item.copy(phase = DesktopTransferPhase.SKIPPED) else item
                },
            )
        }
        completion?.complete(null)
        emit()
    }

    private fun emit() = publish(draft, batch)

    companion object {
        private val failurePhases = setOf(DesktopTransferPhase.FAILED, DesktopTransferPhase.OUTCOME_UNKNOWN)
        private val terminalPhases = failurePhases + setOf(DesktopTransferPhase.COMPLETED, DesktopTransferPhase.SKIPPED)
    }
}
