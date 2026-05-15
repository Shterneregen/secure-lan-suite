package com.shterneregen.securelan.androidclient.network

import com.shterneregen.securelan.androidclient.model.DiscoveredPeer
import com.shterneregen.securelan.androidclient.model.SecureLanPorts
import com.shterneregen.securelan.androidclient.protocol.DiscoveryCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.time.Instant

class PeerDiscoveryRepository(
    private val discoveryPort: Int = SecureLanPorts.DEFAULT_DISCOVERY_PORT,
) {
    fun discoverPeers(): Flow<DiscoveredPeer> = callbackFlow {
        val socket = DatagramSocket(discoveryPort).apply {
            broadcast = true
            soTimeout = 1000
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val receiver: Job = scope.launch {
            val buffer = ByteArray(2048)
            while (!socket.isClosed) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val peer = DiscoveryCodec.decode(packet.data, packet.length, packet.address)
                    trySend(peer.copy(lastSeen = Instant.now()))
                } catch (_: SocketTimeoutException) {
                } catch (_: IllegalArgumentException) {
                }
            }
        }
        awaitClose {
            receiver.cancel()
            socket.close()
        }
    }

    suspend fun announce(peerId: String, nickname: String, chatPort: Int = 1, filePort: Int = 1) {
        val payload = DiscoveryCodec.encode(peerId, nickname, chatPort, filePort)
        DatagramSocket().use { socket ->
            socket.broadcast = true
            val packet = DatagramPacket(payload, payload.size, InetAddress.getByName("255.255.255.255"), discoveryPort)
            socket.send(packet)
        }
    }
}
