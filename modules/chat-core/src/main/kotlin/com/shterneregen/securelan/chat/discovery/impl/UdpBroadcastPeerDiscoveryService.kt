package com.shterneregen.securelan.chat.discovery.impl

import com.shterneregen.securelan.chat.discovery.DiscoveredPeer
import com.shterneregen.securelan.chat.discovery.PeerDiscoveryConfig
import com.shterneregen.securelan.chat.discovery.PeerDiscoveryListener
import com.shterneregen.securelan.chat.discovery.PeerDiscoveryService
import com.shterneregen.securelan.common.net.udp.BroadcastAddressResolver
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/** Each start owns its socket, threads, configuration and callbacks. Stop invalidates that owner. */
class UdpBroadcastPeerDiscoveryService @JvmOverloads constructor(
    private val broadcastAddressResolver: BroadcastAddressResolver = BroadcastAddressResolver(),
    private val send: (DatagramSocket, DatagramPacket) -> Unit = { socket, packet -> socket.send(packet) },
    private val nanoTime: () -> Long = System::nanoTime,
) : PeerDiscoveryService {
    private class Run(val config: PeerDiscoveryConfig, val listener: PeerDiscoveryListener, val socket: DatagramSocket) {
        @Volatile var active = true
        @Volatile var announce = config.announceEnabled
        val peers = ConcurrentHashMap<String, Pair<DiscoveredPeer, Long>>()
        lateinit var receiver: Thread
        lateinit var announcer: Thread
        var lastErrorAt: Long? = null
    }
    private val lock = Any()
    @Volatile private var current: Run? = null

    override fun start(config: PeerDiscoveryConfig, listener: PeerDiscoveryListener) {
        synchronized(lock) {
            stop()
            val socket = DatagramSocket(null)
            try {
                socket.reuseAddress = true
                socket.broadcast = true
                socket.bind(InetSocketAddress(config.discoveryPort))
                val run = Run(config, listener, socket)
                run.receiver = Thread({ receiveLoop(run) }, "securelan-peer-discovery-receiver").apply { isDaemon = true }
                run.announcer = Thread({ announceLoop(run) }, "securelan-peer-discovery-announcer").apply { isDaemon = true }
                current = run
                run.receiver.start()
                run.announcer.start()
            } catch (error: Exception) {
                socket.close()
                current = null
                listener.onDiscoveryError("Unable to start peer discovery", error)
            }
        }
    }

    override fun stop() {
        synchronized(lock) {
            val run = current ?: return
            current = null
            run.active = false
            run.socket.close()
            run.receiver.interrupt()
            run.announcer.interrupt()
            run.peers.clear()
        }
    }

    override fun setAnnounceEnabled(announceEnabled: Boolean) { current?.announce = announceEnabled }
    override fun isRunning(): Boolean = current != null
    override fun snapshot(): List<DiscoveredPeer> = current?.peers?.values?.map { it.first }
        ?.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.nickname }) ?: emptyList()

    private fun active(run: Run) = run.active && current === run

    private fun announceLoop(run: Run) {
        while (active(run)) {
            try {
                if (run.announce) {
                    val config = run.config
                    val data = DiscoveryMessageCodec.toBytes(DiscoveryMessage(config.peerId, config.nickname, config.chatPort, config.filePort))
                    for (address in broadcastAddressResolver.resolve()) {
                        if (!active(run)) return
                        try { send(run.socket, DatagramPacket(data, data.size, address, config.discoveryPort)) }
                        catch (error: Exception) { publishError(run, error) }
                    }
                }
            } catch (error: Exception) { publishError(run, error) }
            // Housekeeping and pacing also run after send/resolver failure.
            synchronized(lock) {
                if (!active(run)) return
                val now = nanoTime()
                for ((id, entry) in run.peers) {
                    if (now - entry.second >= run.config.staleTimeout.toNanos() && run.peers.remove(id, entry)) {
                        run.listener.onPeerExpired(entry.first)
                    }
                }
            }
            try { Thread.sleep(run.config.announceInterval.toMillis().coerceAtLeast(1)) }
            catch (_: InterruptedException) { Thread.currentThread().interrupt(); return }
        }
    }

    private fun receiveLoop(run: Run) {
        val buffer = ByteArray(1024)
        while (active(run)) {
            try {
                val packet = DatagramPacket(buffer, buffer.size)
                run.socket.receive(packet)
                val message = DiscoveryMessageCodec.fromBytes(packet.data, packet.length)
                if (message.peerId == run.config.peerId) continue
                val peer = DiscoveredPeer(message.peerId, message.nickname, packet.address.hostAddress, message.chatPort, message.filePort, Instant.now())
                synchronized(lock) {
                    if (!active(run)) return
                    run.peers[peer.peerId] = peer to nanoTime()
                    run.listener.onPeerDiscovered(peer)
                }
            } catch (error: Exception) { publishError(run, error) }
        }
    }

    private fun publishError(run: Run, error: Exception) {
        synchronized(lock) {
            if (!active(run)) return
            val now = nanoTime()
            if (run.lastErrorAt == null || now - run.lastErrorAt!! >= 5_000_000_000L) {
                run.lastErrorAt = now
                run.listener.onDiscoveryError("Peer discovery IO error", error)
            }
        }
    }
}
