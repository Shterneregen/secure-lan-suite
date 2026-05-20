package com.shterneregen.securelan.filetransfer.quickshare

import java.util.Objects

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

    fun shareId(): String = shareIdValue

    fun snapshot(): QuickShareSnapshot? = snapshotValue

    fun message(): String = messageValue

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
