package com.shterneregen.securelan.filetransfer.quickshare

import java.util.Objects

/**
 * Event published during Quick Share lifecycle changes (e.g. server start/stop, share creation,
 * share expiration, access, download).
 *
 * @param shareId Identifier of the affected share, or empty string for server-level events.
 * @param snapshot Snapshot of the share state at the time of the event, or `null` for server events.
 * @param message Descriptive event message.
 * @param remoteAddress IP or socket address of the requesting client, if applicable.
 */
class QuickShareEvent(
    shareId: String?,
    snapshot: QuickShareSnapshot?,
    message: String?,
    remoteAddress: String?,
) {
    private val shareIdValue: String = shareId ?: ""
    private val snapshotValue: QuickShareSnapshot? = snapshot
    private val messageValue: String = Objects.requireNonNull(message, "message must not be null")!!
    private val remoteAddressValue: String = remoteAddress ?: ""

    /**
     * Unique identifier of the affected share, or empty string for server-level events.
     */
    fun shareId(): String = shareIdValue

    /**
     * Snapshot of the affected share, or `null` if not associated with a specific share.
     */
    fun snapshot(): QuickShareSnapshot? = snapshotValue

    /**
     * Human-readable description of the event.
     */
    fun message(): String = messageValue

    /**
     * Remote client address that triggered this event, if applicable.
     */
    fun remoteAddress(): String = remoteAddressValue

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is QuickShareEvent) return false
        return shareIdValue == other.shareIdValue &&
            snapshotValue == other.snapshotValue &&
            messageValue == other.messageValue &&
            remoteAddressValue == other.remoteAddressValue
    }

    override fun hashCode(): Int = Objects.hash(shareIdValue, snapshotValue, messageValue, remoteAddressValue)

    override fun toString(): String =
        "QuickShareEvent[shareId=$shareIdValue, snapshot=$snapshotValue, message=$messageValue, remoteAddress=$remoteAddressValue]"
}
