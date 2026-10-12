package com.shterneregen.securelan.desktop.compose.state.transfer

import java.nio.file.Path

/**
 * Captured recipient identity and endpoint for one connection generation.
 * [generation] prevents an old draft or paused batch from resuming in a different room.
 */
data class DesktopTransferTarget(
    val generation: Long,
    val peerId: String,
    val nickname: String,
    val host: String,
    val port: Int,
)

/** UI attempt states; only [COMPLETED] represents receiver-confirmed delivery. */
enum class DesktopTransferPhase {
    QUEUED,
    PREPARING,
    WAITING_FOR_ACCEPTANCE,
    SENDING,
    WAITING_FOR_CONFIRMATION,
    COMPLETED,
    FAILED,
    /** Confirmation was lost; retrying may duplicate a file already saved by the recipient. */
    OUTCOME_UNKNOWN,
    SKIPPED,
}

/** Queue states independent of any individual file's outcome. */
enum class DesktopBatchPhase { RUNNING, STOP_REQUESTED, PAUSED, FINISHED }

/**
 * Immutable progress or outcome for one attempt, retained when the user retries.
 *
 * @property id Local attempt identity used to reject stale progress callbacks.
 * @property wireId Transfer ID reported by the underlying operation, when available.
 * @property size Source size in bytes, or `null` before inspection succeeds.
 * @property error Recovery guidance suitable for the UI, without credentials or raw payloads.
 */
data class DesktopTransferAttempt(
    val id: String,
    val wireId: String? = null,
    val phase: DesktopTransferPhase = DesktopTransferPhase.PREPARING,
    val bytes: Long = 0,
    val size: Long? = null,
    val error: String? = null,
)
/** A selected file and its ordered attempt history; removing queued work retains this summary row. */
data class DesktopTransferItem(
    val id: String,
    val path: Path,
    val size: Long? = null,
    val phase: DesktopTransferPhase = DesktopTransferPhase.QUEUED,
    val attempts: List<DesktopTransferAttempt> = emptyList(),
) {
    val name: String get() = path.fileName.toString()
}
/** Unsent review selection bound to its original recipient. */
data class DesktopTransferDraft(val target: DesktopTransferTarget, val items: List<DesktopTransferItem> = emptyList()) {
    /** Total bytes, or `null` if a source size is unknown, invalid, or would overflow [Long]. */
    val knownBytes: Long? get() = transferBytes(items)
}

/** Immutable queue snapshot; counts describe final file outcomes rather than local byte progress. */
data class DesktopTransferBatch(
    val id: String,
    val target: DesktopTransferTarget,
    val items: List<DesktopTransferItem>,
    val phase: DesktopBatchPhase = DesktopBatchPhase.RUNNING,
) {
    val completed: Int get() = items.count { it.phase == DesktopTransferPhase.COMPLETED }
    val failed: Int get() = items.count { it.phase == DesktopTransferPhase.FAILED || it.phase == DesktopTransferPhase.OUTCOME_UNKNOWN }
    val skipped: Int get() = items.count { it.phase == DesktopTransferPhase.SKIPPED }
    val summary: String get() = "Confirmed $completed of ${items.size} for ${target.nickname}: $failed failed or unconfirmed, $skipped skipped"
    val unfinished: Boolean get() = phase != DesktopBatchPhase.FINISHED
}

private fun transferBytes(items: List<DesktopTransferItem>): Long? {
    var total = 0L
    for (item in items) {
        val size = item.size ?: return null
        if (size < 0 || Long.MAX_VALUE - total < size) return null
        total += size
    }
    return total
}
