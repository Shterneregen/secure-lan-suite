package com.shterneregen.securelan.neighbor

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.address.MacAddress
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.ptr.IntByReference

/**
 * Resolves one on-link IPv4 target through the Windows IP Helper API. SendARP does not require
 * administrator rights and performs its own ARP exchange.
 */
internal class WindowsSendArpResolver(private val call: SendArpCall = NativeSendArp) : NeighborMacResolver {
    override fun resolve(query: NeighborQuery): NeighborMacResult {
        val mac = ByteArray(MAC_BYTES)
        val length = IntByReference(MAC_BYTES)
        val result = try {
            call.send(ipv4ToInt(query.target), ipv4ToInt(query.binding.address), mac, length)
        } catch (_: UnsatisfiedLinkError) {
            return NeighborMacResult.Unsupported
        } catch (_: SecurityException) {
            return NeighborMacResult.Failed(NeighborFailure.PERMISSION_DENIED)
        } catch (_: Exception) {
            return NeighborMacResult.Failed(NeighborFailure.UNEXPECTED)
        }
        return when (result) {
            NO_ERROR -> {
                if (length.value < MAC_BYTES) NeighborMacResult.Failed(NeighborFailure.UNEXPECTED)
                else MacAddress.fromBytes(mac)?.takeIf(MacAddress::isWakeTarget)
                    ?.let { NeighborMacResult.Found(it) } ?: NeighborMacResult.NotFound
            }
            ERROR_BAD_NET_RESP, ERROR_GEN_FAILURE -> NeighborMacResult.NotFound
            ERROR_ACCESS_DENIED -> NeighborMacResult.Failed(NeighborFailure.PERMISSION_DENIED)
            ERROR_NOT_SUPPORTED -> NeighborMacResult.Unsupported
            else -> NeighborMacResult.Failed(NeighborFailure.UNEXPECTED)
        }
    }

    internal companion object {
        const val MAC_BYTES = 6
        const val NO_ERROR = 0
        const val ERROR_ACCESS_DENIED = 5
        const val ERROR_GEN_FAILURE = 31
        const val ERROR_NOT_SUPPORTED = 50
        const val ERROR_BAD_NET_RESP = 67

        /** Windows accepts the destination as a network-order numeric address. */
        fun ipv4ToInt(address: IpLiteral): Int {
            val bytes = address.bytes()
            return ((bytes[0].toInt() and 0xFF) shl 24) or
                ((bytes[1].toInt() and 0xFF) shl 16) or
                ((bytes[2].toInt() and 0xFF) shl 8) or
                (bytes[3].toInt() and 0xFF)
        }
    }
}

internal fun interface SendArpCall {
    fun send(destIp: Int, srcIp: Int, macAddress: ByteArray, length: IntByReference): Int
}

private object NativeSendArp : SendArpCall {
    private val api by lazy { Native.load("iphlpapi", IpHlpApi::class.java) }

    override fun send(destIp: Int, srcIp: Int, macAddress: ByteArray, length: IntByReference): Int =
        api.SendARP(destIp, srcIp, macAddress, length)
}

private interface IpHlpApi : Library {
    fun SendARP(destIp: Int, srcIp: Int, macAddress: ByteArray, length: IntByReference): Int
}
