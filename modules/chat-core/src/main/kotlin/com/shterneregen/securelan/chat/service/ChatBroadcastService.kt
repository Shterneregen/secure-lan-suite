package com.shterneregen.securelan.chat.service

import com.shterneregen.securelan.chat.transport.ChatSocketSession

interface ChatBroadcastService {
    fun addClient(nickname: String, session: ChatSocketSession)
    fun removeClient(nickname: String)
    fun syncPeers(session: ChatSocketSession, excludeNickname: String?)
    fun publishUserJoined(nickname: String)
    fun publishUserLeft(nickname: String)
    fun publishMessage(senderNickname: String, text: String)
    fun publishSignal(senderNickname: String, payload: String)
}
