package com.shterneregen.securelan.chat.discovery.impl

import com.shterneregen.securelan.chat.discovery.DiscoveryCodec

internal object DiscoveryMessageCodec {
    @JvmStatic
    fun encode(message: DiscoveryMessage): String = DiscoveryCodec.encode(toPublicMessage(message))

    @JvmStatic
    fun decode(payload: String?): DiscoveryMessage = fromPublicMessage(DiscoveryCodec.decode(payload))

    @JvmStatic
    fun toBytes(message: DiscoveryMessage): ByteArray = DiscoveryCodec.toBytes(toPublicMessage(message))

    @JvmStatic
    fun fromBytes(data: ByteArray, length: Int): DiscoveryMessage = fromPublicMessage(DiscoveryCodec.fromBytes(data, length))

    private fun toPublicMessage(message: DiscoveryMessage): DiscoveryCodec.Message =
        DiscoveryCodec.Message(message.peerId, message.nickname, message.chatPort, message.filePort)

    private fun fromPublicMessage(message: DiscoveryCodec.Message): DiscoveryMessage =
        DiscoveryMessage(message.peerId, message.nickname, message.chatPort, message.filePort)
}
