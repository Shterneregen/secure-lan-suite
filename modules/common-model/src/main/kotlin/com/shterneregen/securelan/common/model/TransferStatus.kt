package com.shterneregen.securelan.common.model

/** Lifecycle state of a file transfer. */
enum class TransferStatus {
    /** Bytes are currently being transferred. */
    IN_PROGRESS,
    /** The transfer completed successfully. */
    COMPLETED,
    /** The transfer failed. */
    FAILED,
    /** The transfer was cancelled. */
    CANCELLED,
}
