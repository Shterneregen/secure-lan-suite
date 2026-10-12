package com.shterneregen.securelan.filetransfer.service

/**
 * Represents a single executable file transfer attempt.
 *
 * Implementations manage network sockets and file streams for a single transfer.
 * Calling [close] aborts the ongoing operation and closes underlying resources.
 */
interface FileTransferOperation : AutoCloseable {
    /**
     * Unique transfer identifier associated with this operation.
     */
    val transferId: String

    /**
     * Executes the transfer operation synchronously.
     *
     * @return [FileTransferOutcome.Completed] on success, or [FileTransferOutcome.Failed] on error.
     */
    fun execute(): FileTransferOutcome

    /**
     * Aborts the transfer operation and releases all allocated network and file resources.
     */
    override fun close()
}

/**
 * Phases that a file transfer operation progresses through during execution.
 */
enum class FileTransferPhase {
    /** Preparing local file handle and inspecting file size. */
    PREPARING,

    /** Connected to receiver; waiting for remote acceptance of the transfer. */
    WAITING_FOR_ACCEPTANCE,

    /** Actively streaming encrypted file chunks to receiver. */
    SENDING,

    /** All bytes transmitted; awaiting receiver's final confirmation acknowledgment. */
    WAITING_FOR_CONFIRMATION,

    /** Transfer completed successfully. */
    COMPLETED,
}

/**
 * Snapshot of progress and state update emitted during an active transfer operation.
 *
 * @property transferId Unique transfer session identifier.
 * @property phase Current lifecycle phase of the transfer operation.
 * @property transferredBytes Total payload bytes transmitted so far.
 * @property totalBytes Total size of the file in bytes, if determined.
 */
data class FileTransferOperationUpdate(
    val transferId: String,
    val phase: FileTransferPhase,
    val transferredBytes: Long,
    val totalBytes: Long?,
)

/**
 * Listener receiving [FileTransferOperationUpdate] events during operation execution.
 */
fun interface FileTransferOperationListener {
    /**
     * Invoked when an operation enters a new phase or reports progress.
     *
     * @param update Current operation status snapshot.
     */
    fun onUpdate(update: FileTransferOperationUpdate)
}

/**
 * Categorization of reasons why a file transfer operation failed.
 */
enum class FileTransferFailureKind {
    /** Receiver explicitly declined the incoming transfer. */
    REJECTED,

    /** Local source file was missing, unreadable, or changed size during transfer. */
    SOURCE,

    /** Handshake or wire protocol violation detected. */
    PROTOCOL,

    /** TCP connection failed, was dropped, or broken. */
    CONNECTION,

    /** Operation exceeded a configured timeout limit. */
    TIMEOUT,

    /** Transfer operation was explicitly closed/canceled locally. */
    ABORTED,

    /** EOF frame was sent but confirmation failed, leaving remote delivery state undetermined. */
    OUTCOME_UNKNOWN,
}

/**
 * Final outcome of a [FileTransferOperation] execution.
 */
sealed interface FileTransferOutcome {
    /**
     * Transfer session identifier.
     */
    val transferId: String

    /**
     * Indicates that the file transfer completed successfully and was confirmed by receiver.
     *
     * @property transferId Transfer session identifier.
     */
    data class Completed(override val transferId: String) : FileTransferOutcome

    /**
     * Indicates that the file transfer failed.
     *
     * @property transferId Transfer session identifier.
     * @property kind Categorized failure reason.
     * @property cause Underlying exception or error, if any.
     */
    data class Failed(
        override val transferId: String,
        val kind: FileTransferFailureKind,
        val cause: Throwable? = null,
    ) : FileTransferOutcome
}

/**
 * Configurable timeout parameters for each phase of a file transfer operation.
 *
 * @property connectMillis Socket connection establishment timeout in milliseconds (defaults to 10,000 ms).
 * @property acceptanceMillis Maximum wait time in milliseconds for the receiver to accept the transfer (defaults to 300,000 ms / 5 min).
 * @property inactivityMillis Maximum allowed silence in milliseconds between received socket frames (defaults to 60,000 ms / 1 min).
 * @property confirmationMillis Maximum wait time in milliseconds for final post-transfer confirmation (defaults to 60,000 ms / 1 min).
 */
data class FileTransferTimeouts(
    val connectMillis: Int = 10_000,
    val acceptanceMillis: Long = 300_000,
    val inactivityMillis: Long = 60_000,
    val confirmationMillis: Long = 60_000,
) {
    init {
        require(connectMillis > 0 && acceptanceMillis > 0 && inactivityMillis > 0 && confirmationMillis > 0) {
            "All timeouts must be strictly positive"
        }
    }
}
