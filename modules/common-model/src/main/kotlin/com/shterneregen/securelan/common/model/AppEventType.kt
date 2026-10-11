package com.shterneregen.securelan.common.model

/**
 * Classifies the kind of event that occurred within the application.
 *
 * Events are emitted by various modules and consumed by the UI or logging layer.
 */
enum class AppEventType {
    /** A general informational notification. */
    INFO,

    /** A non-critical warning that may require attention. */
    WARNING,

    /** A critical error condition. */
    ERROR,

    /** A remote peer has successfully connected. */
    PEER_CONNECTED,

    /** A remote peer has disconnected. */
    PEER_DISCONNECTED,

    /** A chat message was sent to a remote peer. */
    CHAT_MESSAGE_SENT,

    /** A chat message was received from a remote peer. */
    CHAT_MESSAGE_RECEIVED,

    /** A file-transfer session has started. */
    FILE_TRANSFER_STARTED,

    /** Progress has been reported for an ongoing file transfer. */
    FILE_TRANSFER_PROGRESS,

    /** A file transfer has finished successfully. */
    FILE_TRANSFER_COMPLETED,

    /** A file transfer has failed. */
    FILE_TRANSFER_FAILED,
}
