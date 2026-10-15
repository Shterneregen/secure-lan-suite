package com.shterneregen.securelan.filetransfer.service

import java.nio.file.Path
import java.util.Objects

/**
 * Immutable request model containing all parameters required for an outgoing secure file transfer.
 *
 * @property host Remote peer IP address or hostname.
 * @property port Remote peer file transfer server TCP port (1..65535).
 * @property senderId Identifier of the local sending peer.
 * @property recipientId Identifier of the target remote peer.
 * @property sessionPassword Shared session or room password used to authenticate the transfer handshake.
 * @property file Local filesystem path to the file to be transmitted.
 */
@JvmRecord
data class FileTransferClientRequest(
    val host: String,
    val port: Int,
    val senderId: String,
    val recipientId: String,
    val sessionPassword: String,
    val file: Path,
    val authentication: com.shterneregen.securelan.filetransfer.protocol.FileTransferPeerAuthentication,
) {
    init {
        Objects.requireNonNull(host, "host must not be null")
        Objects.requireNonNull(senderId, "senderId must not be null")
        Objects.requireNonNull(recipientId, "recipientId must not be null")
        Objects.requireNonNull(sessionPassword, "sessionPassword must not be null")
        Objects.requireNonNull(file, "file must not be null")
        require(host.isNotBlank()) { "host must not be blank" }
        require(port in 1..65_535) { "port must be between 1 and 65535" }
        require(senderId.isNotBlank()) { "senderId must not be blank" }
        require(recipientId.isNotBlank()) { "recipientId must not be blank" }
    }
    override fun toString(): String = "FileTransferClientRequest[host=$host, port=$port, senderId=$senderId, recipientId=$recipientId, file=$file, sessionPassword=<redacted>]"
}
