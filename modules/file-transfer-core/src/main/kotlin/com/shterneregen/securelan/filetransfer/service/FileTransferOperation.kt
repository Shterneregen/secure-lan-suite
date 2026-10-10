package com.shterneregen.securelan.filetransfer.service

/** One attempt. Allocate before launching the worker; close on session disconnect. */
interface FileTransferOperation : AutoCloseable {
    val transferId: String
    fun execute(): FileTransferOutcome
    override fun close()
}

enum class FileTransferPhase {
    PREPARING, WAITING_FOR_ACCEPTANCE, SENDING, WAITING_FOR_CONFIRMATION, COMPLETED,
}

data class FileTransferOperationUpdate(
    val transferId: String,
    val phase: FileTransferPhase,
    val transferredBytes: Long,
    val totalBytes: Long?,
)

fun interface FileTransferOperationListener {
    fun onUpdate(update: FileTransferOperationUpdate)
}

enum class FileTransferFailureKind {
    REJECTED, SOURCE, PROTOCOL, CONNECTION, TIMEOUT, ABORTED, OUTCOME_UNKNOWN,
}

sealed interface FileTransferOutcome {
    val transferId: String

    data class Completed(override val transferId: String) : FileTransferOutcome
    data class Failed(
        override val transferId: String,
        val kind: FileTransferFailureKind,
        val cause: Throwable? = null,
    ) : FileTransferOutcome
}

data class FileTransferTimeouts(
    val connectMillis: Int = 10_000,
    val acceptanceMillis: Long = 300_000,
    val inactivityMillis: Long = 60_000,
    val confirmationMillis: Long = 60_000,
) {
    init {
        require(connectMillis > 0 && acceptanceMillis > 0 && inactivityMillis > 0 && confirmationMillis > 0)
    }
}
