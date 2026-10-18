package com.shterneregen.securelan.chat.connection

import com.shterneregen.securelan.common.net.address.IpLiteral

/** Untrusted connection draft. The optional password is memory-only and is never an identity proof. */
data class RoomConnectionDescriptor(
    val primaryAddress: IpLiteral,
    val chatPort: Int,
    val filePortHint: Int? = null,
    val peerIdHint: String? = null,
    val roomInstanceIdHint: String? = null,
    val displayNameHint: String? = null,
    val alternativeAddresses: List<IpLiteral> = emptyList(),
    val password: String? = null,
) {
    override fun toString(): String = "RoomConnectionDescriptor(primaryAddress=$primaryAddress, chatPort=$chatPort, " +
        "filePortHint=$filePortHint, peerIdHint=$peerIdHint, roomInstanceIdHint=$roomInstanceIdHint, " +
        "displayNameHint=$displayNameHint, alternativeAddresses=$alternativeAddresses, password=${if (password == null) null else "<redacted>"})"
}
