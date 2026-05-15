package com.shterneregen.securelan.androidclient.ui

import com.shterneregen.securelan.androidclient.model.AppLogEntry
import com.shterneregen.securelan.androidclient.model.DiscoveredPeer
import com.shterneregen.securelan.androidclient.model.PeerRole
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object AndroidUiFormatters {
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())
    private val logTimestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault())

    fun formatBytes(bytes: Long): String {
        val safeBytes = bytes.coerceAtLeast(0)
        if (safeBytes < 1024) return "$safeBytes B"

        val units = listOf("KB", "MB", "GB")
        var value = safeBytes / 1024.0
        var unitIndex = 0
        while (value >= 1024 && unitIndex < units.lastIndex) {
            value /= 1024.0
            unitIndex++
        }
        return "%.1f %s".format(value, units[unitIndex])
    }

    fun formatTimestamp(timestamp: Instant): String = timeFormatter.format(timestamp)

    fun formatLogEntry(entry: AppLogEntry): String =
        "${logTimestampFormatter.format(entry.timestamp)} [${entry.level.padEnd(5)}] ${entry.message}"

    fun peerEndpointSummary(peer: DiscoveredPeer): String {
        val chatEndpoint = "${peer.host}:${peer.chatPort}"
        val fileEndpoint = "${peer.fileTargetHost}:${peer.fileTargetPort}"
        return if (peer.role == PeerRole.SERVER || fileEndpoint == "${peer.host}:${peer.filePort}") {
            "$chatEndpoint · file ${peer.filePort}"
        } else {
            "$chatEndpoint · file $fileEndpoint"
        }
    }
}
