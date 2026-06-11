package com.shterneregen.securelan.desktop.ui

import com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities
import java.time.Instant
import java.util.Objects

class PeerPresence @JvmOverloads constructor(
    private val nickname: String,
    online: Boolean,
    peerId: String?,
    host: String?,
    chatPort: Int,
    filePort: Int,
    lastSeen: Instant?,
    capabilities: PeerCapabilities = PeerCapabilities.unknown(),
) {
    private var online: Boolean = online
    private var peerId: String? = peerId
    private var host: String? = host
    private var chatPort: Int = chatPort
    private var filePort: Int = filePort
    private var lastSeen: Instant? = lastSeen
    private var capabilities: PeerCapabilities = capabilities

    @JvmOverloads
    fun apply(
        online: Boolean,
        peerId: String?,
        host: String?,
        chatPort: Int,
        filePort: Int,
        lastSeen: Instant?,
        capabilities: PeerCapabilities = PeerCapabilities.unknown(),
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
        if (capabilities != PeerCapabilities.unknown() && !Objects.equals(this.capabilities, capabilities)) {
            this.capabilities = capabilities
            changed = true
            if (capabilities.supportsFileReceive() && capabilities.fileReceivePort() > 0 && this.filePort != capabilities.fileReceivePort()) {
                this.filePort = capabilities.fileReceivePort()
            }
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

    fun capabilities(): PeerCapabilities = capabilities

    fun hasFileEndpoint(): Boolean = !host.isNullOrBlank() && filePort > 0 &&
        (
            capabilities == PeerCapabilities.unknown() ||
                capabilities.supportsFileReceive() ||
                capabilities.platformKind() == PeerCapabilities.PlatformKind.DESKTOP
            )

    fun discovered(): Boolean = !host.isNullOrBlank() && chatPort > 0 && filePort > 0
} 
