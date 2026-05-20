package com.shterneregen.securelan.filetransfer.service

import java.nio.file.Path
import java.util.Objects

@JvmRecord
data class FileTransferClientRequest(
    val host: String,
    val port: Int,
    val senderId: String,
    val recipientId: String,
    val sessionPassword: String,
    val file: Path,
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
}
