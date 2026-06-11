package com.shterneregen.securelan.chat.event

import com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities

@JvmRecord
data class ChatConnectedEvent(val nickname: String?, val remoteAddress: String?, val capabilities: PeerCapabilities) : ChatCoreEvent {
    constructor(nickname: String?, remoteAddress: String?) : this(nickname, remoteAddress, PeerCapabilities.unknown())
}
