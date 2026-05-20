package com.shterneregen.securelan.chat.service.impl

import com.shterneregen.securelan.chat.protocol.WireMessage
import com.shterneregen.securelan.chat.protocol.WireMessageType
import com.shterneregen.securelan.chat.service.ChatBroadcastService
import com.shterneregen.securelan.chat.service.ChatHistoryService
import com.shterneregen.securelan.chat.transport.ChatSocketSession
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class InMemoryChatBroadcastService(private val historyService: ChatHistoryService) : ChatBroadcastService {
    private val clients = ConcurrentHashMap<String, ChatSocketSession>()

    override fun addClient(nickname: String, session: ChatSocketSession) {
        clients[nickname] = session
    }

    override fun removeClient(nickname: String) {
        clients.remove(nickname)
    }

    override fun syncPeers(session: ChatSocketSession, excludeNickname: String?) {
        clients.keys
            .asSequence()
            .filter { nickname -> excludeNickname == null || !nickname.equals(excludeNickname, ignoreCase = true) }
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
            .forEach { nickname -> writeQuietly(session, WireMessage(WireMessageType.USER_JOINED, nickname, "")) }
    }

    override fun publishUserJoined(nickname: String) {
        val line = "[system] $nickname joined the chat"
        historyService.append(line)
        broadcast(WireMessage(WireMessageType.USER_JOINED, nickname, ""))
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
