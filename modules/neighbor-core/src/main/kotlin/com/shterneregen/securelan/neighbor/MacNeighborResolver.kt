package com.shterneregen.securelan.neighbor

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.address.MacAddress
import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.LongByReference
import java.net.NetworkInterface

/**
 * Reads the macOS IPv4 routing table with RTF_LLINFO entries through sysctl. The table contains
 * both ARP entries and other link-layer routes, so the parser requires an IPv4 destination and a
 * six-byte AF_LINK gateway for the same message.
 */
internal class MacNeighborResolver(
    private val sysctl: SysctlCall = NativeSysctl,
    private val probe: NeighborProbe = UdpNeighborProbe(),
    private val sleeper: (Long) -> Unit = { millis -> Thread.sleep(millis) },
    private val attempts: Int = 3,
    private val retryDelayMillis: Long = 100,
    private val interfaceIndex: (String) -> Int? = { name ->
        runCatching { NetworkInterface.getByName(name)?.index }.getOrNull()
    },
) : NeighborMacResolver {
    override fun resolve(query: NeighborQuery): NeighborMacResult {
        val expectedIndex = interfaceIndex(query.binding.interfaceKey)
        probe.trigger(query)
        repeat(attempts.coerceAtLeast(1)) { attempt ->
            val bytes = try {
                sysctl.read(MIB)
            } catch (_: UnsatisfiedLinkError) {
                return NeighborMacResult.Unsupported
            } catch (_: SecurityException) {
                return NeighborMacResult.Failed(NeighborFailure.PERMISSION_DENIED)
            } catch (_: Exception) {
                return NeighborMacResult.Failed(NeighborFailure.IO)
            } ?: return NeighborMacResult.Failed(NeighborFailure.IO)
            parseMacNeighborTable(bytes, query.target, expectedIndex)?.let { return NeighborMacResult.Found(it) }
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

    internal companion object {
        private const val CTL_NET = 4
        private const val PF_ROUTE = 17
        private const val AF_INET = 2
        private const val NET_RT_FLAGS = 2
        private const val RTF_LLINFO = 0x400
        private const val AF_LINK = 18
        private const val RT_MSGHDR_SIZE = 92
        private const val RTA_DST_BIT = 0
        private const val RTA_GATEWAY_BIT = 1

        internal val MIB = intArrayOf(CTL_NET, PF_ROUTE, 0, AF_INET, NET_RT_FLAGS, RTF_LLINFO)

        /** Parses one bounded sysctl snapshot and never trusts a message to contain all sockaddrs. */
        internal fun parseMacNeighborTable(
            bytes: ByteArray,
            target: IpLiteral,
            expectedInterfaceIndex: Int? = null,
        ): MacAddress? {
            val targetBytes = target.bytes()
            var offset = 0
            while (offset + RT_MSGHDR_SIZE <= bytes.size) {
                val messageLength = u16(bytes, offset)
                if (messageLength < RT_MSGHDR_SIZE || offset + messageLength > bytes.size) return null
                val messageInterfaceIndex = u16(bytes, offset + 4)
                if (expectedInterfaceIndex != null && messageInterfaceIndex != 0 &&
                    messageInterfaceIndex != expectedInterfaceIndex) {
                    offset += messageLength
                    continue
                }
                val addressMask = i32(bytes, offset + 12)
                var position = offset + RT_MSGHDR_SIZE
                val messageEnd = offset + messageLength
                var destination: ByteArray? = null
                var hardware: MacAddress? = null
                var bit = 0
                while (bit < 32 && position + 2 <= messageEnd && position < messageEnd) {
                    if ((addressMask and (1 shl bit)) == 0) {
                        bit++
                        continue
                    }
                    val length = bytes[position].toInt() and 0xFF
                    if (length < 2 || position + length > messageEnd) break
                    val family = bytes[position + 1].toInt() and 0xFF
                    when (family) {
                        AF_INET -> if (bit == RTA_DST_BIT && length >= 8) {
                            destination = bytes.copyOfRange(position + 4, position + 8)
                        }
                        AF_LINK -> if (bit == RTA_GATEWAY_BIT && length >= 8) {
                            val nameLength = bytes[position + 5].toInt() and 0xFF
                            val addressLength = bytes[position + 6].toInt() and 0xFF
                            val addressStart = position + 8 + nameLength
                            if (addressLength == 6 && addressStart + 6 <= messageEnd) {
                                hardware = MacAddress.fromBytes(bytes.copyOfRange(addressStart, addressStart + 6))
                            }
                        }
                    }
                    val advance = (length + 3) and 3.inv()
                    if (advance <= 0) break
                    position += advance
                    bit++
                }
                if (destination != null && hardware != null && destination.contentEquals(targetBytes)) {
                    return hardware.takeIf(MacAddress::isWakeTarget)
                }
                offset += messageLength
            }
            return null
        }

        private fun u16(bytes: ByteArray, offset: Int): Int =
            (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)

        private fun i32(bytes: ByteArray, offset: Int): Int =
            (bytes[offset].toInt() and 0xFF) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 3].toInt() and 0xFF) shl 24)
    }
}

internal fun interface SysctlCall {
    fun read(mib: IntArray): ByteArray?
}

private object NativeSysctl : SysctlCall {
    private const val MAX_TABLE_BYTES = 1 shl 20
    private val libc by lazy { Native.load("c", LibC::class.java) }

    override fun read(mib: IntArray): ByteArray? {
        val size = LongByReference(0)
        if (libc.sysctl(mib, mib.size, null, size, null, 0) != 0) return null
        val length = size.value
        if (length !in 1L..MAX_TABLE_BYTES.toLong()) return null
        val memory = Memory(length)
        val actual = LongByReference(length)
        if (libc.sysctl(mib, mib.size, memory, actual, null, 0) != 0) return null
        val read = actual.value
        if (read !in 1L..length) return null
        return memory.getByteArray(0, read.toInt())
    }
}

private interface LibC : Library {
    fun sysctl(name: IntArray, namelen: Int, oldp: Pointer?, oldlenp: LongByReference,
        newp: Pointer?, newlen: Long): Int
}
