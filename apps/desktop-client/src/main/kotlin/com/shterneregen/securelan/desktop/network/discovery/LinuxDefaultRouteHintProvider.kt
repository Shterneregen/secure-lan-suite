package com.shterneregen.securelan.desktop.network.discovery

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/**
 * Reads an optional Linux IPv4 default-route interface recommendation without running commands.
 *
 * This application adapter is intended for a network-info provider's hint callback. It owns no
 * sockets, retains no cached topology and never selects a WoL source or changes process routing.
 * Windows/macOS, inaccessible procfs, oversized input and ambiguous routes return null.
 *
 * @param routeTable Linux procfs route table; injectable for local fixture tests.
 * @param osName Operating-system name; non-Linux systems never access [routeTable].
 */
class LinuxDefaultRouteHintProvider(
    private val routeTable: Path = Path.of("/proc/net/route"),
    private val osName: String = System.getProperty("os.name", ""),
) {
    /** Returns the unique lowest-metric UP default-route interface, or no recommendation. */
    fun interfaceKey(): String? {
        if (!osName.equals("Linux", ignoreCase = true)) return null
        return try {
            val bytes = Files.newInputStream(routeTable).use { it.readNBytes(MAX_BYTES + 1) }
            // Procfs often reports size zero, so bound actual reads rather than trusting Files.size.
            if (bytes.size > MAX_BYTES) return null
            parse(String(bytes, StandardCharsets.US_ASCII).lineSequence().toList())
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }

    companion object {
        private const val MAX_BYTES = 65_536
        private const val MAX_ROWS = 512
        private val whitespace = Regex("\\s+")
        private val interfaceName = Regex("[a-zA-Z0-9_.:@-]{1,256}")
        private val header = listOf("Iface", "Destination", "Gateway", "Flags", "RefCnt", "Use", "Metric", "Mask")

        /** Parses bounded procfs columns; equal metrics on different interfaces remain ambiguous. */
        internal fun parse(lines: List<String>): String? {
            if (lines.size > MAX_ROWS + 2 || lines.firstOrNull()?.trim()?.split(whitespace)?.take(8) != header) return null
            val rows = lines.drop(1).filter(String::isNotBlank)
            if (rows.size > MAX_ROWS) return null
            val candidates = rows.mapNotNull { line ->
                val fields = line.trim().split(whitespace)
                if (fields.size < 8 || !interfaceName.matches(fields[0])) return@mapNotNull null
                if (fields[1] != "00000000" || fields[7] != "00000000") return@mapNotNull null
                val flags = fields[3].takeIf { it.length <= 8 }?.toLongOrNull(16) ?: return@mapNotNull null
                // RTF_UP is required; RTF_REJECT routes must never become a positive recommendation.
                if (flags and 0x1L == 0L || flags and 0x200L != 0L) return@mapNotNull null
                val metric = fields[6].toLongOrNull()?.takeIf { it >= 0 } ?: return@mapNotNull null
                fields[0] to metric
            }
            val metric = candidates.minOfOrNull { it.second } ?: return null
            return candidates.filter { it.second == metric }.map { it.first }.distinct().singleOrNull()
        }
    }
}
