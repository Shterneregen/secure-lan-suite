package com.shterneregen.securelan.androidclient.ui

import com.shterneregen.securelan.androidclient.model.AppLogEntry
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Presentation-only formatting for byte counts, local timestamps and diagnostic endpoints. */
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

    fun roomEndpoint(address: String, chatPort: String): String = "$address:$chatPort"

    fun formatLogEntry(entry: AppLogEntry): String =
        "${logTimestampFormatter.format(entry.timestamp)} [${entry.level.padEnd(5)}] ${entry.message}"
}
