package com.shterneregen.securelan.desktop.ui

import java.nio.file.Path
import java.util.Locale

object DesktopMainViewHelpers {
    @JvmStatic
    fun suggestedStegoOutputPath(coverPath: Path): Path {
        val fileName = coverPath.fileName.toString()
        val extensionIndex = fileName.lowercase(Locale.ROOT).lastIndexOf(".bmp")
        val outputName = if (extensionIndex > 0) {
            fileName.substring(0, extensionIndex) + "-stego.bmp"
        } else {
            "$fileName-stego.bmp"
        }
        val parent = coverPath.parent
        return (parent?.resolve(outputName) ?: Path.of(outputName)).toAbsolutePath().normalize()
    }

    @JvmStatic
    fun resolveLocalFilePort(
        serverRunning: Boolean,
        serverFilePortText: String,
        clientFilePortText: String,
        defaultFileTransferPort: Int,
        clientFilePortOffset: Int,
    ): Int {
        if (serverRunning) {
            return serverFilePortText.toInt()
        }
        val remoteFilePort = clientFilePortText.toInt()
        val candidate = remoteFilePort + clientFilePortOffset
        return if (candidate > MAX_PORT) defaultFileTransferPort + clientFilePortOffset else candidate
    }

    @JvmStatic
    fun fileTransferErrorMessage(error: Throwable): String {
        val candidate = error.cause ?: error
        val message = candidate.message
        return if (message.isNullOrBlank()) candidate.javaClass.simpleName else message
    }

    @JvmStatic
    fun hostFromRemoteAddress(remoteAddress: String?): String {
        var value = remoteAddress?.trim().orEmpty()
        if (value.isBlank()) {
            return ""
        }
        val slash = value.lastIndexOf('/')
        if (slash >= 0) {
            value = value.substring(slash + 1)
        }
        if (value.startsWith("[")) {
            val closing = value.indexOf(']')
            return if (closing > 0) value.substring(1, closing) else value
        }
        val colon = value.lastIndexOf(':')
        return if (colon > 0) value.substring(0, colon) else value
    }

    @JvmStatic
    fun samePeer(peer: PeerPresence, nickname: String?, peerId: String?): Boolean {
        if (!peerId.isNullOrBlank() && !peer.peerId().isNullOrBlank()) {
            return peer.peerId() == peerId
        }
        return peer.nickname().equals(nickname, ignoreCase = true)
    }

    private const val MAX_PORT = 65_535
}
