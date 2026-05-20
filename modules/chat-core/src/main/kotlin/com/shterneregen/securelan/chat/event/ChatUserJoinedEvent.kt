package com.shterneregen.securelan.chat.event

@JvmRecord
data class ChatUserJoinedEvent(val nickname: String?, val remoteAddress: String?) : ChatCoreEvent {
    constructor(nickname: String?) : this(nickname, "")
}
