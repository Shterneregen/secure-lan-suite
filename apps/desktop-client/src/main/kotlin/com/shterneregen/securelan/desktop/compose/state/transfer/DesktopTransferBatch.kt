package com.shterneregen.securelan.desktop.compose.state.transfer

import java.nio.file.Path

data class DesktopTransferTarget(val generation: Long, val peerId: String, val nickname: String, val host: String, val port: Int)
enum class DesktopTransferPhase { QUEUED, PREPARING, WAITING_FOR_ACCEPTANCE, SENDING, WAITING_FOR_CONFIRMATION, COMPLETED, FAILED, OUTCOME_UNKNOWN, SKIPPED }
enum class DesktopBatchPhase { RUNNING, STOP_REQUESTED, PAUSED, FINISHED }
data class DesktopTransferAttempt(
    val id: String,
    val wireId: String? = null,
    val phase: DesktopTransferPhase = DesktopTransferPhase.PREPARING,
    val bytes: Long = 0,
    val size: Long? = null,
    val error: String? = null,
)
data class DesktopTransferItem(
    val id: String,
    val path: Path,
    val size: Long? = null,
    val phase: DesktopTransferPhase = DesktopTransferPhase.QUEUED,
    val attempts: List<DesktopTransferAttempt> = emptyList(),
) {
    val name: String get() = path.fileName.toString()
}
data class DesktopTransferDraft(val target: DesktopTransferTarget, val items: List<DesktopTransferItem> = emptyList()) {
    val knownBytes: Long? get() = transferBytes(items)
}
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
