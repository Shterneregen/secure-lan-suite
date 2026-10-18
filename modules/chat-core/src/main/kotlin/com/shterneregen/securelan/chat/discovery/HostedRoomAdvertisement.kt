package com.shterneregen.securelan.chat.discovery

/** Construct only after the chat listener is ready. Contains no credential or authenticated identity. */
data class HostedRoomAdvertisement(
    val peerId: String,
    val roomInstanceId: String,
    val displayName: String?,
    val chatPort: Int,
    val activeFilePort: Int?,
    val discoverable: Boolean,
) {
    init {
        require(DiscoveryValidation.uuid(peerId) && DiscoveryValidation.uuid(roomInstanceId))
        require(displayName == null || DiscoveryValidation.displayName(displayName))
        require(chatPort in 1..65535 && (activeFilePort == null || activeFilePort in 1..65535 && activeFilePort != chatPort))
    }
}
