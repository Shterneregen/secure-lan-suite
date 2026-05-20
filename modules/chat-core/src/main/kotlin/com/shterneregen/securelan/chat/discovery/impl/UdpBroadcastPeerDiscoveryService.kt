package com.shterneregen.securelan.chat.discovery.impl

import com.shterneregen.securelan.chat.discovery.DiscoveredPeer
import com.shterneregen.securelan.chat.discovery.PeerDiscoveryConfig
import com.shterneregen.securelan.chat.discovery.PeerDiscoveryListener
import com.shterneregen.securelan.chat.discovery.PeerDiscoveryService
import com.shterneregen.securelan.common.net.udp.BroadcastAddressResolver
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketException
import java.time.Instant
import java.util.Comparator
import java.util.Objects
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class UdpBroadcastPeerDiscoveryService @JvmOverloads constructor(
    broadcastAddressResolver: BroadcastAddressResolver = BroadcastAddressResolver(),
) : PeerDiscoveryService {
    private val broadcastAddressResolver: BroadcastAddressResolver = Objects.requireNonNull(broadcastAddressResolver, "broadcastAddressResolver")
    private val running = AtomicBoolean(false)
    private val announceEnabled = AtomicBoolean(false)
    private val peers = ConcurrentHashMap<String, DiscoveredPeer>()

    @Volatile
    private var socket: DatagramSocket? = null
    private var receiverThread: Thread? = null
    private var announcerThread: Thread? = null
    private lateinit var config: PeerDiscoveryConfig
    private var listener: PeerDiscoveryListener? = null

    override fun start(config: PeerDiscoveryConfig, listener: PeerDiscoveryListener) {
        if (running.get()) {
            stop()
        }
        this.config = config
        this.listener = listener
        announceEnabled.set(config.announceEnabled)
        peers.clear()
        try {
            val createdSocket = DatagramSocket(null)
            createdSocket.reuseAddress = true
            createdSocket.broadcast = true
            createdSocket.bind(InetSocketAddress(config.discoveryPort))
            socket = createdSocket
            running.set(true)
            receiverThread = Thread(this::receiveLoop, "securelan-peer-discovery-receiver").apply {
                isDaemon = true
                start()
            }
            announcerThread = Thread(this::announceLoop, "securelan-peer-discovery-announcer").apply {
                isDaemon = true
                start()
            }
        } catch (e: IOException) {
            closeSocket()
            publishError("Unable to start peer discovery", e)
        }
    }

    override fun stop() {
        running.set(false)
        announceEnabled.set(false)
        closeSocket()
        peers.clear()
    }

    override fun setAnnounceEnabled(announceEnabled: Boolean) {
        this.announceEnabled.set(announceEnabled)
    }

    override fun isRunning(): Boolean = running.get()

    override fun snapshot(): List<DiscoveredPeer> = peers.values
        .sortedWith(Comparator.comparing(DiscoveredPeer::nickname, String.CASE_INSENSITIVE_ORDER))

    private fun announceLoop() {
        while (running.get()) {
            try {
                if (announceEnabled.get()) {
                    announceOnce()
                }
                expireStalePeers()
                Thread.sleep(config.announceInterval.toMillis())
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                return
            } catch (e: Exception) {
                publishError("Unable to announce peer discovery message", e)
            }
        }
    }

    private fun receiveLoop() {
        val buffer = ByteArray(RECEIVE_BUFFER_SIZE)
        while (running.get()) {
            val packet = DatagramPacket(buffer, buffer.size)
            try {
                socket?.receive(packet)
                handlePacket(packet)
            } catch (e: SocketException) {
                if (running.get()) {
                    publishError("Peer discovery socket error", e)
                }
            } catch (e: Exception) {
                publishError("Unable to process peer discovery message", e)
            }
        }
    }

    private fun handlePacket(packet: DatagramPacket) {
        val message = DiscoveryMessageCodec.fromBytes(packet.data, packet.length)
        if (config.peerId == message.peerId) {
            return
        }
        val peer = DiscoveredPeer(
            message.peerId,
            message.nickname,
            packet.address.hostAddress,
            message.chatPort,
            message.filePort,
            Instant.now(),
        )
        peers[peer.peerId] = peer
        listener?.onPeerDiscovered(peer)
    }

    @Throws(IOException::class)
    private fun announceOnce() {
        val data = DiscoveryMessageCodec.toBytes(
            DiscoveryMessage(config.peerId, config.nickname, config.chatPort, config.filePort),
        )
        for (address in broadcastAddressResolver.resolve()) {
            val packet = DatagramPacket(data, data.size, address, config.discoveryPort)
            socket?.send(packet)
        }
    }

    private fun expireStalePeers() {
        val cutoff = Instant.now().minus(config.staleTimeout)
        for (peer in ArrayList(peers.values)) {
            if (peer.lastSeen.isBefore(cutoff) && peers.remove(peer.peerId, peer)) {
                listener?.onPeerExpired(peer)
            }
        }
    }

    private fun closeSocket() {
        socket?.close()
        socket = null
    }

    private fun publishError(message: String, cause: Throwable) {
        listener?.onDiscoveryError(message, cause)
    }

    companion object {
        private const val RECEIVE_BUFFER_SIZE = 1024
    }
}
