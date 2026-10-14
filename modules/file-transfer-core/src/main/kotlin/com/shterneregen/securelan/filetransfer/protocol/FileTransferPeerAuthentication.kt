package com.shterneregen.securelan.filetransfer.protocol

import com.shterneregen.securelan.crypto.protocol.PeerIdentity
import java.io.IOException

/**
 * Current room identity and a resolver populated exclusively from authenticated room presence.
 * Discovery, IP equality and the room password alone cannot certify another participant's key.
 * Applications must make the resolver fail closed on disconnect/peer departure.
 */
class FileTransferPeerAuthentication(
    val localId: String,
    val identity: PeerIdentity,
    private val peerKey: (String) -> String?,
) {
    fun verify(peer: String, payload: ByteArray, proof: String) {
        val key = peerKey(peer)?.takeIf { it.isNotBlank() } ?: throw IOException("File peer is not authenticated in the current room")
        PeerIdentity.verify(key, payload, proof)
    }
}
