package com.shterneregen.securelan.common.model

import java.math.BigInteger

/**
 * A snapshot of a file transfer's byte counts and [status].
 *
 * @property transferId identifier of the transfer.
 * @property transferredBytes number of bytes transferred so far; must be non-negative.
 * @property totalBytes total size in bytes; zero may represent an empty or unknown size.
 * @property status current transfer state.
 * @throws IllegalArgumentException if a byte count is negative or a positive total is exceeded.
 */
@JvmRecord
data class FileTransferProgress(
    val transferId: String,
    val transferredBytes: Long,
    val totalBytes: Long,
    val status: TransferStatus,
) {
    init {
        require(transferredBytes >= 0) { "transferredBytes must not be negative" }
        require(totalBytes >= 0) { "totalBytes must not be negative" }
        require(!(totalBytes > 0 && transferredBytes > totalBytes)) { "transferredBytes must not exceed totalBytes" }
    }

    /** Returns the completed percentage, truncated to an integer; returns zero for a zero total. */
    fun percent(): Int {
        if (totalBytes == 0L) return 0
        return BigInteger.valueOf(transferredBytes)
            .multiply(BigInteger.valueOf(100))
            .divide(BigInteger.valueOf(totalBytes))
            .toInt()
    }
}
