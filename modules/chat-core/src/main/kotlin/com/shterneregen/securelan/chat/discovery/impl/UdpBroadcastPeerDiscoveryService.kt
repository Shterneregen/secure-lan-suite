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

/**
 * UDP broadcast-based implementation of [PeerDiscoveryService].
 *
 * ## How it works
 * Two daemon threads are started on [start]:
 * - **Receiver thread** — binds to [PeerDiscoveryConfig.discoveryPort] with `SO_REUSEADDR` and
 *   `SO_BROADCAST`, reads incoming UDP datagrams, and calls [PeerDiscoveryListener.onPeerDiscovered]
 *   for each valid packet from a different peer.
 * - **Announcer thread** — periodically encodes this peer's identity as a [DiscoveryMessage],
 *   broadcasts it to all local network interfaces, and evicts stale peers whose
 *   [DiscoveredPeer.lastSeen] is older than [PeerDiscoveryConfig.staleTimeout].
 *
 * Announcing can be paused/resumed at any time via [setAnnounceEnabled] without restarting the service.
 *
 * ## Thread safety
 * The peer map is a [ConcurrentHashMap]; state flags use [AtomicBoolean].  All public methods
 * are safe to call from any thread.
 *
 * @param broadcastAddressResolver Resolves the broadcast addresses for all active network
 *                                 interfaces.  Defaults to the no-arg [BroadcastAddressResolver].
 */
class UdpBroadcastPeerDiscoveryService @JvmOverloads constructor(
    broadcastAddressResolver: BroadcastAddressResolver = BroadcastAddressResolver(),
) : PeerDiscoveryService {

    private val broadcastAddressResolver: BroadcastAddressResolver =
        Objects.requireNonNull(broadcastAddressResolver, "broadcastAddressResolver")

    private val running = AtomicBoolean(false)
    private val announceEnabled = AtomicBoolean(false)
    private val peers = ConcurrentHashMap<String, DiscoveredPeer>()

    @Volatile
    private var socket: DatagramSocket? = null
    private var receiverThread: Thread? = null
    private var announcerThread: Thread? = null
    private lateinit var config: PeerDiscoveryConfig
    private var listener: PeerDiscoveryListener? = null

    /**
     * Starts the discovery service.
     *
     * If the service is already running, it is stopped and restarted with the new configuration.
     * On socket creation failure an error is reported via [PeerDiscoveryListener.onDiscoveryError]
     * and the service is left in the stopped state.
     *
     * @param config   Discovery configuration.
     * @param listener Event callback for peer and error notifications.
     */
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

    /**
     * Stops the discovery service, closes the UDP socket, and clears all known peers.
     *
     * Closing the socket will unblock the receiver thread.  The announcer thread is woken
     * by the socket close because `receive()` throws [SocketException].
     */
    override fun stop() {
        running.set(false)
        announceEnabled.set(false)
        closeSocket()
        peers.clear()
    }

    /**
     * Enables or disables outbound UDP announcements without restarting the service.
     *
     * @param announceEnabled `true` to broadcast presence; `false` to stop broadcasting.
     */
    override fun setAnnounceEnabled(announceEnabled: Boolean) {
        this.announceEnabled.set(announceEnabled)
    }

    /** Returns `true` if the service is currently running. */
    override fun isRunning(): Boolean = running.get()

    /**
     * Returns a snapshot of all currently known peers sorted alphabetically by nickname
     * (case-insensitive).
     *
     * @return An immutable list of the current peer snapshots.
     */
    override fun snapshot(): List<DiscoveredPeer> = peers.values
        .sortedWith(Comparator.comparing(DiscoveredPeer::nickname, String.CASE_INSENSITIVE_ORDER))

    // -------------------------------------------------------------------------
    // Background thread logic
    // -------------------------------------------------------------------------

    /**
     * Loop executed by the announcer thread.
     *
     * Broadcasts this peer's identity on each announce-interval tick (if announcing is enabled)
     * and evicts stale entries after every interval.
     */
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

    /**
     * Loop executed by the receiver thread.
     *
     * Blocks on [DatagramSocket.receive] and delegates each valid datagram to [handlePacket].
     * A [SocketException] that occurs while the service is still running is reported as an error;
     * one that occurs after [stop] is called (i.e. due to socket closure) is silently ignored.
     */
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

    /**
     * Processes a received UDP datagram.
     *
     * Packets from this peer itself (matched by [PeerDiscoveryConfig.peerId]) are silently
     * discarded.  For all others, the peer map is updated and [PeerDiscoveryListener.onPeerDiscovered]
     * is invoked.
     *
     * @param packet The received datagram.
     */
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

    /**
     * Sends a single UDP discovery broadcast to all resolved broadcast addresses.
     *
     * @throws IOException if any broadcast send fails.
     */
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

    /**
     * Removes peers whose [DiscoveredPeer.lastSeen] is older than [PeerDiscoveryConfig.staleTimeout].
     *
     * [PeerDiscoveryListener.onPeerExpired] is called for each evicted peer.
     */
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
        /** Maximum UDP datagram size to buffer in the receiver thread. */
        private const val RECEIVE_BUFFER_SIZE = 1024
    }
}
