package com.shterneregen.securelan.neighbor

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.address.MacAddress
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/**
 * Reads the Linux IPv4 neighbor table through procfs. No commands, sockets for reading, or
 * privileges are required; an optional UDP probe populates a missing entry first.
 */
internal class LinuxNeighborResolver(
    private val arpTable: Path = Path.of("/proc/net/arp"),
    private val probe: NeighborProbe = UdpNeighborProbe(),
    private val sleeper: (Long) -> Unit = { millis -> Thread.sleep(millis) },
    private val attempts: Int = 3,
    private val retryDelayMillis: Long = 100,
) : NeighborMacResolver {
    override fun resolve(query: NeighborQuery): NeighborMacResult {
        probe.trigger(query)
        repeat(attempts.coerceAtLeast(1)) { attempt ->
            val found = try {
                parseLinuxArpTable(readTable(), query.target, query.binding.interfaceKey)
            } catch (_: SecurityException) {
                return NeighborMacResult.Failed(NeighborFailure.PERMISSION_DENIED)
            } catch (_: IOException) {
                return NeighborMacResult.Failed(NeighborFailure.IO)
            }
            if (found != null) return NeighborMacResult.Found(found)
            if (attempt < attempts - 1) {
                try {
                    sleeper(retryDelayMillis)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return NeighborMacResult.Failed(NeighborFailure.IO)
                }
            }
        }
        return NeighborMacResult.NotFound
    }

    private fun readTable(): String {
        val bytes = Files.newInputStream(arpTable).use { it.readNBytes(MAX_BYTES + 1) }
        if (bytes.size > MAX_BYTES) throw IOException("Neighbor table is too large")
        return String(bytes, StandardCharsets.US_ASCII)
    }

    internal companion object {
        const val MAX_BYTES = 64 * 1024
        private val whitespace = Regex("\\s+")
        private const val ATF_COM = 0x2L

        /** Parses bounded procfs rows and returns only a complete, unicast entry for the target. */
        internal fun parseLinuxArpTable(text: String, target: IpLiteral, interfaceKey: String?): MacAddress? {
            val rows = text.lineSequence().filter(String::isNotBlank).dropWhile { it.trimStart().startsWith("IP address") }
            var seen = 0
            for (line in rows) {
                if (++seen > MAX_ROWS) return null
                val fields = line.trim().split(whitespace)
                if (fields.size < 6 || fields[0] != target.value) continue
                if (interfaceKey != null && fields[5] != interfaceKey) continue
                val flags = fields[2].removePrefix("0x").removePrefix("0X").toLongOrNull(16) ?: continue
                if ((flags and ATF_COM) == 0L) continue
                val mac = MacAddress.parse(fields[3])?.takeIf(MacAddress::isWakeTarget) ?: continue
                return mac
            }
            return null
        }

        private const val MAX_ROWS = 512
    }
}
