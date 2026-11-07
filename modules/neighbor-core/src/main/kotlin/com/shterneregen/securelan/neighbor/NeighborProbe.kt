package com.shterneregen.securelan.neighbor

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress

/** Best-effort traffic that makes the operating system populate an ARP/neighbor entry. */
internal fun interface NeighborProbe {
    fun trigger(query: NeighborQuery)
}

/** A zero-length UDP datagram is enough to start address resolution on a directly connected LAN. */
internal class UdpNeighborProbe : NeighborProbe {
    override fun trigger(query: NeighborQuery) {
        try {
            DatagramSocket(null).use { socket ->
                socket.bind(InetSocketAddress(InetAddress.getByAddress(query.binding.address.bytes()), 0))
                socket.send(DatagramPacket(ByteArray(0), 0,
                    InetAddress.getByAddress(query.target.bytes()), PROBE_PORT))
            }
        } catch (_: Exception) {
            // A table entry may already exist; the caller reports the eventual lookup result.
        }
    }

    private companion object {
        const val PROBE_PORT = 9
    }
}
