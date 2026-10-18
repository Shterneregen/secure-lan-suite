package com.shterneregen.securelan.androidclient.network

import com.shterneregen.securelan.androidclient.protocol.WireMessage
import com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities
import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope
import com.shterneregen.securelan.crypto.protocol.PeerIdentity

/** Injectable authenticated room transport; platform sockets remain behind this boundary. */
interface RoomClient : AutoCloseable {
    fun setRoute(route: LanSocketRoute?) {}
    val fileIdentity: PeerIdentity
    suspend fun connect(host: String, port: Int, requestedNickname: String, sessionPassword: String, capabilities: PeerCapabilities): String
    suspend fun sendMessage(text: String)
    suspend fun readMessage(): WireMessage?
    suspend fun sendSignal(signal: RtcSignalEnvelope)
    suspend fun disconnect()
}
