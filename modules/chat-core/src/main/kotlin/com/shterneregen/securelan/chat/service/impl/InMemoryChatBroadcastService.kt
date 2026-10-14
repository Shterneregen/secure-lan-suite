package com.shterneregen.securelan.chat.service.impl

import com.shterneregen.securelan.chat.protocol.WireMessage
import com.shterneregen.securelan.chat.protocol.WireMessageType
import com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities
import com.shterneregen.securelan.chat.service.ChatBroadcastService
import com.shterneregen.securelan.chat.service.ChatHistoryService
import com.shterneregen.securelan.chat.transport.ChatSocketSession
import com.shterneregen.securelan.common.model.rtc.RtcSignalCodec
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory, thread-safe implementation of [ChatBroadcastService].
 *
 * Connected clients and their capabilities are stored in [ConcurrentHashMap]s keyed by nickname.
 * Broadcast operations enqueue independently to bounded per-session writers. Overflow or
 * write failure closes that recipient, so a blocked peer cannot stall the fanout loop.
 *
 * Signal routing enforces two security constraints:
 * 1. The `fromPeer` field of every signal is **always overwritten** with [senderNickname] so
 *    that clients cannot spoof each other.
 * 2. Signals with a blank target or an unknown target nickname are silently dropped.
 *
 * @param historyService The [ChatHistoryService] where chat and system lines are recorded.
 */
class InMemoryChatBroadcastService(private val historyService: ChatHistoryService) : ChatBroadcastService {

    private val clients = ConcurrentHashMap<String, ChatSocketSession>()
    private val clientCapabilities = ConcurrentHashMap<String, PeerCapabilities>()
    private val deliveries = ConcurrentHashMap<ChatSocketSession, java.util.concurrent.ThreadPoolExecutor>()

    /**
     * Registers [session] under [nickname] so it receives future broadcasts.
     *
     * @param nickname     The unique, registry-approved nickname of the client.
     * @param session      The active TCP session.
     * @param capabilities The capabilities the client advertised during the handshake.
     */
    override fun addClient(nickname: String, session: ChatSocketSession, capabilities: PeerCapabilities) {
        clients[nickname] = session
        clientCapabilities[nickname] = capabilities
    }

    /**
     * Removes [nickname] from the broadcast list and the capabilities map.
     *
     * @param nickname The nickname of the client to remove.
     */
    override fun removeClient(nickname: String) {
        clients.remove(nickname)?.let { deliveries.remove(it)?.shutdownNow() }
        clientCapabilities.remove(nickname)
    }

    /**
     * Sends a [WireMessageType.USER_JOINED] frame for each currently connected peer to [session],
     * sorted alphabetically, so that the new client receives a complete peer list.
     *
     * @param session         The session of the joining client.
     * @param excludeNickname The joining client's own nickname (excluded from the sync); `null`
     *                        to include all clients.
     */
    override fun syncPeers(session: ChatSocketSession, excludeNickname: String?) {
        clients.keys
            .asSequence()
            .filter { nickname -> excludeNickname == null || !nickname.equals(excludeNickname, ignoreCase = true) }
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
            .forEach { nickname ->
                val capabilities = clientCapabilities[nickname] ?: PeerCapabilities.unknown()
                writeQuietly(session, WireMessage(WireMessageType.USER_JOINED, nickname, capabilities.encode()))
            }
    }

    /**
     * Broadcasts a join event and a system announcement, then records the event in history.
     *
     * @param nickname     The nickname of the client that joined.
     * @param capabilities The capabilities of the joining client.
     */
    override fun publishUserJoined(nickname: String, capabilities: PeerCapabilities) {
        val line = "[system] $nickname joined the chat"
        historyService.append(line)
        broadcast(WireMessage(WireMessageType.USER_JOINED, nickname, capabilities.encode()))
        broadcast(WireMessage(WireMessageType.SYSTEM, "system", line))
    }

    /**
     * Broadcasts a leave event and a system announcement, then records the event in history.
     *
     * @param nickname The nickname of the client that left.
     */
    override fun publishUserLeft(nickname: String) {
        val line = "[system] $nickname left the chat"
        historyService.append(line)
        broadcast(WireMessage(WireMessageType.USER_LEFT, nickname, ""))
        broadcast(WireMessage(WireMessageType.SYSTEM, "system", line))
    }

    /**
     * Broadcasts a chat message to all connected clients and records it in history.
     *
     * @param senderNickname The nickname of the client who sent the message.
     * @param text           The message text.
     */
    override fun publishMessage(senderNickname: String, text: String) {
        val line = "$senderNickname: $text"
        historyService.append(line)
        broadcast(WireMessage(WireMessageType.CHAT, senderNickname, text))
    }

    /**
     * Routes a WebRTC signalling message to the addressed peer.
     *
     * The [payload] is deserialized, the sender is canonicalized to [senderNickname], and the
     * resulting message is delivered to the target peer's session.  Signals with an invalid
     * payload, a blank `sessionId`, or a blank/unknown target peer are silently dropped.
     *
     * @param senderNickname The authenticated sender's nickname.
     * @param payload        The raw serialized [com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope].
     */
    override fun publishSignal(senderNickname: String, payload: String) {
        val signal = runCatching { RtcSignalCodec.deserialize(payload).withSender(senderNickname) }.getOrNull()
            ?: return
        if (signal.toPeer().isBlank() || signal.sessionId().isBlank()) return
        val recipient = clients.entries
            .firstOrNull { it.key.equals(signal.toPeer(), ignoreCase = true) }?.value ?: return
        writeQuietly(recipient, WireMessage(WireMessageType.SIGNAL, senderNickname, RtcSignalCodec.serialize(signal)))
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun broadcast(message: WireMessage) {
        clients.forEach { (_, session) -> writeQuietly(session, message) }
    }

    /**
     * Writes [message] to [session], swallowing any [IOException].
     *
     * Individual write failures (e.g. a client that has already disconnected) must not
     * propagate and disrupt the broadcast loop for other clients.
     */
    private fun writeQuietly(session: ChatSocketSession, message: WireMessage) {
        val delivery = deliveries.computeIfAbsent(session) {
            java.util.concurrent.ThreadPoolExecutor(1, 1, 30, java.util.concurrent.TimeUnit.SECONDS,
                java.util.concurrent.ArrayBlockingQueue(32),
                { task -> Thread(task, "chat-peer-delivery").apply { isDaemon = true } }).apply { allowCoreThreadTimeOut(true) }
        }
        try {
            delivery.execute {
                try { session.writeMessage(message) } catch (_: IOException) {
                    runCatching { session.close() }
                    deliveries.remove(session, delivery)
                    delivery.shutdownNow()
                }
            }
        } catch (_: java.util.concurrent.RejectedExecutionException) {
            runCatching { session.close() }
            deliveries.remove(session, delivery)
            delivery.shutdownNow()
        }
    }
}
