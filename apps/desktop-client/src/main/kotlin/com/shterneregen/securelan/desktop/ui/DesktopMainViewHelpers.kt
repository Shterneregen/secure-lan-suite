package com.shterneregen.securelan.desktop.ui

import com.shterneregen.securelan.chat.discovery.PeerDiscoveryConfig
import com.shterneregen.securelan.chat.discovery.DiscoveredPeer
import com.shterneregen.securelan.common.model.rtc.RtcSessionState
import com.shterneregen.securelan.stego.model.BmpCapacity
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.file.Path
import java.util.Locale
import javax.imageio.ImageIO

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
    fun ensureBmpExtension(path: Path): Path {
        val text = path.toString()
        return if (isBmpPath(path)) path else Path.of("$text.bmp").toAbsolutePath().normalize()
    }

    @JvmStatic
    @Throws(IOException::class)
    fun readImageAsBmpBytes(imagePath: Path): ByteArray {
        if (isBmpPath(imagePath)) {
            return java.nio.file.Files.readAllBytes(imagePath)
        }
        val image = ImageIO.read(imagePath.toFile()) ?: throw IOException("Unsupported image file: $imagePath")
        val rgbImage = BufferedImage(image.width, image.height, BufferedImage.TYPE_3BYTE_BGR)
        val graphics = rgbImage.graphics
        try {
            graphics.drawImage(image, 0, 0, null)
        } finally {
            graphics.dispose()
        }
        val output = ByteArrayOutputStream()
        if (!ImageIO.write(rgbImage, "bmp", output)) {
            throw IOException("BMP writer is unavailable")
        }
        return output.toByteArray()
    }

    @JvmStatic
    fun formatStegoCapacity(capacity: BmpCapacity): String =
        "Capacity: %d bytes payload in %dx%d %d-bit BMP".format(
            capacity.payloadCapacityBytes,
            capacity.width,
            capacity.height,
            capacity.bitsPerPixel,
        )

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
    fun resolveInferredClientFilePort(
        hostFilePortText: String,
        defaultFileTransferPort: Int,
        clientFilePortOffset: Int,
    ): Int {
        val hostFilePort = hostFilePortText.toInt()
        val candidate = hostFilePort + clientFilePortOffset
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

    @JvmStatic
    fun discoveryStartedMessage(discoveryConfig: PeerDiscoveryConfig): String =
        if (discoveryConfig.announceEnabled) {
            "[discovery] broadcasting as ${discoveryConfig.nickname} on UDP ${discoveryConfig.discoveryPort}"
        } else {
            "[discovery] room is hidden; listening on UDP ${discoveryConfig.discoveryPort} without broadcasting"
        }

    @JvmStatic
    fun discoveryListeningMessage(discoveryPort: Int): String = "[discovery] listening on UDP $discoveryPort"

    @JvmStatic
    fun discoveryErrorDiagnostics(message: String, cause: Throwable?): String =
        "[discovery-error] $message" + if (cause == null) "" else " -> ${cause.message}"

    @JvmStatic
    fun discoveryChatMessage(message: String): String = "[discovery] $message"

    @JvmStatic
    fun discoverySearchHint(): String =
        "Looking for SecureLanSuite peers on this LAN. Select a discovered peer and connect before sending files or starting a call."

    @JvmStatic
    fun discoveryVisibilityMessage(discoverable: Boolean): String =
        if (discoverable) {
            "[discovery] room is now discoverable"
        } else {
            "[discovery] room is now hidden from automatic discovery"
        }

    @JvmStatic
    fun discoveryPeerFoundDiagnostics(peer: DiscoveredPeer): String =
        "[discovery] ${peer.nickname} at ${peer.host}:${peer.chatPort}"

    @JvmStatic
    fun discoveryPeerExpiredDiagnostics(peer: DiscoveredPeer): String =
        "[discovery] expired ${peer.nickname} at ${peer.host}"

    @JvmStatic
    fun localNetworkInfoMessage(localIps: List<String>): String = when (localIps.size) {
        0 -> "[info] local network IP is unavailable right now"
        1 -> "[info] local network IP: ${localIps.first()}"
        else -> "[info] local network IPs: ${localIps.joinToString(", ")}"
    }

    @JvmStatic
    fun localNetworkInfoErrorMessage(message: String?): String = "[info] failed to determine local network IP: $message"

    @JvmStatic
    fun hangUpAvailable(state: RtcSessionState?): Boolean = when (state) {
        null,
        RtcSessionState.IDLE,
        RtcSessionState.CLOSED,
        RtcSessionState.FAILED,
        RtcSessionState.UNAVAILABLE,
        -> false
        RtcSessionState.NEGOTIATING,
        RtcSessionState.CONNECTING,
        RtcSessionState.CONNECTED,
        RtcSessionState.CLOSING,
        -> true
    }

    @JvmStatic
    fun selectedPeerFileCapable(peer: PeerPresence?): Boolean = peer?.online() == true && peer.discovered()

    @JvmStatic
    fun selectedPeerCallable(peer: PeerPresence?): Boolean = peer?.online() == true

    private fun isBmpPath(path: Path): Boolean = path.toString().lowercase(Locale.ROOT).endsWith(".bmp")

    private const val MAX_PORT = 65_535
}
