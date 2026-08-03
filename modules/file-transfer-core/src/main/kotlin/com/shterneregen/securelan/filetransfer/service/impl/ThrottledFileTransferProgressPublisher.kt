package com.shterneregen.securelan.filetransfer.service.impl

import com.shterneregen.securelan.common.model.FileTransferProgress
import com.shterneregen.securelan.common.model.TransferStatus
import com.shterneregen.securelan.filetransfer.event.FileTransferProgressEvent
import com.shterneregen.securelan.filetransfer.service.FileTransferEventPublisher

/** Keeps per-chunk progress updates from overwhelming UI event queues during fast transfers. */
internal class ThrottledFileTransferProgressPublisher(
    private val eventPublisher: FileTransferEventPublisher,
    private val transferId: String,
    private val totalBytes: Long,
    private val outgoing: Boolean,
    private val minimumIntervalNanos: Long = DEFAULT_MINIMUM_INTERVAL_NANOS,
    private val nanoTime: () -> Long = System::nanoTime,
) {
    private var lastPublishedAt = 0L
    private var lastPublishedBytes = -1L

    init {
        require(minimumIntervalNanos >= 0) { "minimumIntervalNanos must not be negative" }
    }

    fun report(transferredBytes: Long) {
        if (transferredBytes == lastPublishedBytes) return

        val now = nanoTime()
        val firstUpdate = lastPublishedBytes < 0
        val completed = totalBytes > 0 && transferredBytes >= totalBytes
        val intervalElapsed = now - lastPublishedAt >= minimumIntervalNanos
        if (!firstUpdate && !completed && !intervalElapsed) return

        eventPublisher.publish(
            FileTransferProgressEvent(
                transferId,
                FileTransferProgress(transferId, transferredBytes, totalBytes, TransferStatus.IN_PROGRESS),
                outgoing,
            ),
        )
        lastPublishedAt = now
        lastPublishedBytes = transferredBytes
    }

    private companion object {
        private const val DEFAULT_MINIMUM_INTERVAL_NANOS = 100_000_000L
    }
}
