package com.shterneregen.securelan.chat.event

import com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities

@JvmRecord
data class ChatUserJoinedEvent(val nickname: String?, val remoteAddress: String?, val capabilities: PeerCapabilities) : ChatCoreEvent {
    constructor(nickname: String?, remoteAddress: String?) : this(nickname, remoteAddress, PeerCapabilities.unknown())
    constructor(nickname: String?) : this(nickname, "", PeerCapabilities.unknown())
}
