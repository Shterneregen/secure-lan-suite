package com.shterneregen.securelan.androidclient.protocol

import com.shterneregen.securelan.androidclient.model.DiscoveredPeer
import com.shterneregen.securelan.chat.discovery.DiscoveryCodec as CoreDiscoveryCodec
import java.net.InetAddress

object DiscoveryCodec {
    fun encode(peerId: String, nickname: String, chatPort: Int, filePort: Int): ByteArray {
        return CoreDiscoveryCodec.toBytes(CoreDiscoveryCodec.Message(peerId, nickname, chatPort, filePort))
    }

    fun decode(bytes: ByteArray, length: Int, address: InetAddress): DiscoveredPeer {
        val message = CoreDiscoveryCodec.fromBytes(bytes, length)
        return DiscoveredPeer(
            peerId = message.peerId(),
            nickname = message.nickname(),
            host = address.hostAddress ?: address.hostName,
            chatPort = message.chatPort(),
            filePort = message.filePort(),
        )
    }
}
