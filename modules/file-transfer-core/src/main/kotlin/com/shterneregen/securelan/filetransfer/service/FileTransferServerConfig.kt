package com.shterneregen.securelan.filetransfer.service

import java.nio.file.Path
import java.util.Objects

class FileTransferServerConfig(
    private val port: Int,
    storageDirectory: Path,
    sessionPassword: String,
    acceptanceHandler: FileTransferAcceptanceHandler?,
) {
    private val storageDirectory: Path = Objects.requireNonNull(storageDirectory, "storageDirectory must not be null")
    private val sessionPassword: String = Objects.requireNonNull(sessionPassword, "sessionPassword must not be null")
    private val acceptanceHandler: FileTransferAcceptanceHandler = acceptanceHandler ?: FileTransferAcceptanceHandler.acceptAll()

    constructor(port: Int, storageDirectory: Path, sessionPassword: String) : this(port, storageDirectory, sessionPassword, FileTransferAcceptanceHandler.acceptAll())

    init {
        require(port in 1..65_535) { "port must be between 1 and 65535" }
        require(sessionPassword.isNotBlank()) { "sessionPassword must not be blank" }
    }

    fun port(): Int = port
    fun storageDirectory(): Path = storageDirectory
    fun sessionPassword(): String = sessionPassword
    fun acceptanceHandler(): FileTransferAcceptanceHandler = acceptanceHandler

    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is FileTransferServerConfig &&
                port == other.port &&
                storageDirectory == other.storageDirectory &&
                sessionPassword == other.sessionPassword &&
                acceptanceHandler == other.acceptanceHandler)

    override fun hashCode(): Int = Objects.hash(port, storageDirectory, sessionPassword, acceptanceHandler)

    override fun toString(): String =
        "FileTransferServerConfig[port=$port, storageDirectory=$storageDirectory, sessionPassword=$sessionPassword, acceptanceHandler=$acceptanceHandler]"
}
