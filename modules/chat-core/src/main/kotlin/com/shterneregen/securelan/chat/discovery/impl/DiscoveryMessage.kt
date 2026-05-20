package com.shterneregen.securelan.chat.discovery.impl

internal data class DiscoveryMessage(
    val peerId: String,
    val nickname: String,
    val chatPort: Int,
    val filePort: Int,
)
