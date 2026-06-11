package com.shterneregen.securelan.chat.service.impl

import com.shterneregen.securelan.chat.protocol.WireMessage
import com.shterneregen.securelan.chat.protocol.WireMessageType
import com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities
import com.shterneregen.securelan.chat.service.ChatBroadcastService
import com.shterneregen.securelan.chat.service.ChatHistoryService
import com.shterneregen.securelan.chat.transport.ChatSocketSession
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class InMemoryChatBroadcastService(private val historyService: ChatHistoryService) : ChatBroadcastService {
    private val clients = ConcurrentHashMap<String, ChatSocketSession>()
    private val clientCapabilities = ConcurrentHashMap<String, PeerCapabilities>()

    override fun addClient(nickname: String, session: ChatSocketSession, capabilities: PeerCapabilities) {
        clients[nickname] = session
        clientCapabilities[nickname] = capabilities
    }

    override fun removeClient(nickname: String) {
        clients.remove(nickname)
        clientCapabilities.remove(nickname)
    }

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

    override fun publishUserJoined(nickname: String, capabilities: PeerCapabilities) {
        val line = "[system] $nickname joined the chat"
        historyService.append(line)
        broadcast(WireMessage(WireMessageType.USER_JOINED, nickname, capabilities.encode()))
        broadcast(WireMessage(WireMessageType.SYSTEM, "system", line))
    }

    override fun publishUserLeft(nickname: String) {
        val line = "[system] $nickname left the chat"
        historyService.append(line)
        broadcast(WireMessage(WireMessageType.USER_LEFT, nickname, ""))
        broadcast(WireMessage(WireMessageType.SYSTEM, "system", line))
    }

    override fun publishMessage(senderNickname: String, text: String) {
        val line = "$senderNickname: $text"
        historyService.append(line)
        broadcast(WireMessage(WireMessageType.CHAT, senderNickname, text))
    }

    override fun publishSignal(senderNickname: String, payload: String) {
        broadcast(WireMessage(WireMessageType.SIGNAL, senderNickname, payload))
    }

    private fun broadcast(message: WireMessage) {
        clients.forEach { (_, session) -> writeQuietly(session, message) }
    }

    private fun writeQuietly(session: ChatSocketSession, message: WireMessage) {
        try {
            session.writeMessage(message)
        } catch (_: IOException) {
        }
    }
}
