package com.shterneregen.securelan.filetransfer.service.impl

import com.shterneregen.securelan.common.model.FileTransferProgress
import com.shterneregen.securelan.common.model.TransferStatus
import com.shterneregen.securelan.filetransfer.event.FileTransferProgressEvent
import com.shterneregen.securelan.filetransfer.service.FileTransferEventPublisher

/**
 * Throttles per-chunk progress update events to prevent flooding UI event queues during fast local transfers.
 *
 * Guarantees that the first update and the final 100% completion update are always emitted immediately,
 * while intermediate updates are emitted at most once per [minimumIntervalNanos].
 *
 * @param eventPublisher Destination publisher for progress events.
 * @param transferId Unique transfer session identifier.
 * @param totalBytes Declared total size in bytes.
 * @param outgoing `true` if this peer is sending; `false` if receiving.
 * @param minimumIntervalNanos Minimum elapsed interval in nanoseconds between successive progress events.
 * @param nanoTime Source for elapsed time in nanoseconds (defaults to [System.nanoTime]).
 */
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

    /**
     * Reports current transferred bytes. Emits an event if threshold/completion criteria are met.
     *
     * @param transferredBytes Total bytes transferred so far.
     */
    fun report(transferredBytes: Long) {
        if (transferredBytes == lastPublishedBytes) return

        val now = nanoTime()
        val firstUpdate = lastPublishedBytes < 0
        val completed = totalBytes in 1..transferredBytes
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
