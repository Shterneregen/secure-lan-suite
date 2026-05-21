package com.shterneregen.securelan.desktop.ui

import java.time.Instant
import java.util.Objects

class PeerPresence(
    private val nickname: String,
    online: Boolean,
    peerId: String?,
    host: String?,
    chatPort: Int,
    filePort: Int,
    lastSeen: Instant?,
) {
    private var online: Boolean = online
    private var peerId: String? = peerId
    private var host: String? = host
    private var chatPort: Int = chatPort
    private var filePort: Int = filePort
    private var lastSeen: Instant? = lastSeen

    fun apply(
        online: Boolean,
        peerId: String?,
        host: String?,
        chatPort: Int,
        filePort: Int,
        lastSeen: Instant?,
    ): Boolean {
        var changed = this.online != online
        this.online = online
        if (!peerId.isNullOrBlank() && !Objects.equals(this.peerId, peerId)) {
            this.peerId = peerId
            changed = true
        }
        if (!host.isNullOrBlank() && !Objects.equals(this.host, host)) {
            this.host = host
            changed = true
        }
        if (chatPort > 0 && this.chatPort != chatPort) {
            this.chatPort = chatPort
            changed = true
        }
        if (filePort > 0 && this.filePort != filePort) {
            this.filePort = filePort
            changed = true
        }
        if (lastSeen != null && !Objects.equals(this.lastSeen, lastSeen)) {
            this.lastSeen = lastSeen
            changed = true
        }
        return changed
    }

    fun markOffline(): Boolean {
        val changed = online
        online = false
        return changed
    }

    fun nickname(): String = nickname

    fun online(): Boolean = online

    fun peerId(): String? = peerId

    fun host(): String? = host

    fun chatPort(): Int = chatPort

    fun filePort(): Int = filePort

    fun lastSeen(): Instant? = lastSeen

    fun discovered(): Boolean = !host.isNullOrBlank() && chatPort > 0 && filePort > 0
}
