package com.shterneregen.securelan.desktop.ui

/**
 * Mutable transfer-event accumulator with throttled average-speed calculation.
 * Publish [snapshot] copies to Compose so subsequent progress cannot change an earlier UI state.
 *
 * @param nanoTime Monotonic clock used for elapsed time; injectable for deterministic speed tests.
 */
class TransferEntry @JvmOverloads constructor(
    @JvmField val transferId: String,
    @JvmField val fileName: String,
    @JvmField val outgoing: Boolean,
    @JvmField var status: String,
    @JvmField var percent: Int,
    @JvmField var totalBytes: Long,
    private val nanoTime: () -> Long = System::nanoTime,
) {
    private val startNanos: Long = nanoTime()

    @JvmField
    var lastTransferredBytes: Long = 0

    @JvmField
    var lastSpeedDisplayNanos: Long = 0

    @JvmField
    var speedBytesPerSecond: Double = 0.0

    fun active(): Boolean = status == "Sending" || status == "Receiving"

    fun directionLabel(): String = if (outgoing) "↑ Sent" else "↓ Received"

    /** Updates byte progress and refreshes average speed at most once per display interval. */
    fun updateProgress(transferredBytes: Long, percent: Int, totalBytes: Long) {
        val now = nanoTime()
        val elapsedNanos = now - startNanos
        if (transferredBytes >= 0 && elapsedNanos > 0 &&
            (speedBytesPerSecond == 0.0 ||
                now - lastSpeedDisplayNanos >= SPEED_DISPLAY_INTERVAL_NANOS ||
                percent >= 100)
        ) {
            speedBytesPerSecond = transferredBytes * NANOS_PER_SECOND / elapsedNanos
            lastSpeedDisplayNanos = now
        }
        this.percent = percent
        this.totalBytes = totalBytes
        lastTransferredBytes = transferredBytes
    }

    /** Clears the live speed after a terminal transfer event. */
    fun stopSpeedTracking() {
        speedBytesPerSecond = 0.0
    }

    /** Returns an independent display copy; it is not a continuation of this entry's timing clock. */
    fun snapshot(): TransferEntry = TransferEntry(
        transferId, fileName, outgoing, status, percent, totalBytes,
    ).also { copy ->
        copy.lastTransferredBytes = lastTransferredBytes
        copy.lastSpeedDisplayNanos = lastSpeedDisplayNanos
        copy.speedBytesPerSecond = speedBytesPerSecond
    }

    private companion object {
        const val SPEED_DISPLAY_INTERVAL_NANOS = 750_000_000L
        const val NANOS_PER_SECOND = 1_000_000_000.0
    }
}
