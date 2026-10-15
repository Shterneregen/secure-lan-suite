package com.shterneregen.securelan.chat.service

/** Local socket-write outcome. WRITTEN is not a server or recipient delivery acknowledgement. */
enum class ChatSendOutcome { WRITTEN, EMPTY, DISCONNECTED, FAILED }
