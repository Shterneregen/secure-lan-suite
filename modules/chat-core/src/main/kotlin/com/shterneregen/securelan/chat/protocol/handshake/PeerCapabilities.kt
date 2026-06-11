package com.shterneregen.securelan.chat.protocol.handshake

import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Locale
import java.util.Objects

class PeerCapabilities @JvmOverloads constructor(
    platform: String?,
    appVersion: String?,
    supportsFileReceive: Boolean,
    fileReceivePort: Int,
    supportsVoice: Boolean,
    supportsVideo: Boolean,
    deviceName: String? = "",
    protocolVersion: Int = CURRENT_PROTOCOL_VERSION,
    supportsFileSend: Boolean = true,
    supportsQuickShare: Boolean = false,
    supportsSteganography: Boolean = false,
    supportsRtcDataChannel: Boolean = false,
    clientId: String? = "",
    operatingSystem: String? = "",
    metadata: Map<String, String> = emptyMap(),
) {
    private val platform: String = normalizeText(platform).ifBlank { PLATFORM_UNKNOWN }
    private val appVersion: String = normalizeText(appVersion)
    private val deviceName: String = normalizeText(deviceName)
    private val clientId: String = normalizeText(clientId)
    private val operatingSystem: String = normalizeText(operatingSystem)
    private val metadata: Map<String, String> = metadata
        .filterKeys { it.isNotBlank() }
        .mapKeys { normalizeKey(it.key) }
        .mapValues { normalizeText(it.value) }
        .toSortedMap()

    private val supportsFileReceive: Boolean = supportsFileReceive
    private val fileReceivePort: Int = if (fileReceivePort in 1..65_535) fileReceivePort else 0
    private val supportsVoice: Boolean = supportsVoice
    private val supportsVideo: Boolean = supportsVideo
    private val protocolVersion: Int = protocolVersion.coerceAtLeast(1)
    private val supportsFileSend: Boolean = supportsFileSend
    private val supportsQuickShare: Boolean = supportsQuickShare
    private val supportsSteganography: Boolean = supportsSteganography
    private val supportsRtcDataChannel: Boolean = supportsRtcDataChannel

    fun platform(): String = platform
    fun appVersion(): String = appVersion
    fun supportsFileReceive(): Boolean = supportsFileReceive
    fun fileReceivePort(): Int = fileReceivePort
    fun supportsVoice(): Boolean = supportsVoice
    fun supportsVideo(): Boolean = supportsVideo
    fun deviceName(): String = deviceName
    fun protocolVersion(): Int = protocolVersion
    fun supportsFileSend(): Boolean = supportsFileSend
    fun supportsQuickShare(): Boolean = supportsQuickShare
    fun supportsSteganography(): Boolean = supportsSteganography
    fun supportsRtcDataChannel(): Boolean = supportsRtcDataChannel
    fun clientId(): String = clientId
    fun operatingSystem(): String = operatingSystem
    fun metadata(): Map<String, String> = metadata

    fun withFileReceiver(fileReceivePort: Int, enabled: Boolean = true): PeerCapabilities = PeerCapabilities(
        platform,
        appVersion,
        enabled,
        fileReceivePort,
        supportsVoice,
        supportsVideo,
        deviceName,
        protocolVersion,
        supportsFileSend,
        supportsQuickShare,
        supportsSteganography,
        supportsRtcDataChannel,
        clientId,
        operatingSystem,
        metadata,
    )

    fun encode(): String {
        val flags = buildFlags()
        return ENCODED_PREFIX + listOf(
            platformToken(platform),
            encodeText(appVersion),
            fileReceivePort.toString(),
            flags.toString(),
            encodeText(deviceName),
            protocolVersion.toString(),
        ).joinToString("|")
    }

    private fun buildFlags(): Int {
        var flags = 0
        if (supportsFileReceive) flags = flags or FLAG_FILE_RECEIVE
        if (supportsFileSend) flags = flags or FLAG_FILE_SEND
        if (supportsVoice) flags = flags or FLAG_VOICE
        if (supportsVideo) flags = flags or FLAG_VIDEO
        if (supportsQuickShare) flags = flags or FLAG_QUICK_SHARE
        if (supportsSteganography) flags = flags or FLAG_STEGANOGRAPHY
        if (supportsRtcDataChannel) flags = flags or FLAG_RTC_DATA_CHANNEL
        return flags
    }

    fun platformKind(): PlatformKind = PlatformKind.from(platform)

    override fun equals(other: Any?): Boolean = this === other ||
        other is PeerCapabilities &&
        platform == other.platform &&
        appVersion == other.appVersion &&
        supportsFileReceive == other.supportsFileReceive &&
        fileReceivePort == other.fileReceivePort &&
        supportsVoice == other.supportsVoice &&
        supportsVideo == other.supportsVideo &&
        deviceName == other.deviceName &&
        protocolVersion == other.protocolVersion &&
        supportsFileSend == other.supportsFileSend &&
        supportsQuickShare == other.supportsQuickShare &&
        supportsSteganography == other.supportsSteganography &&
        supportsRtcDataChannel == other.supportsRtcDataChannel &&
        clientId == other.clientId &&
        operatingSystem == other.operatingSystem &&
        metadata == other.metadata

    override fun hashCode(): Int = Objects.hash(
        platform,
        appVersion,
        supportsFileReceive,
        fileReceivePort,
        supportsVoice,
        supportsVideo,
        deviceName,
        protocolVersion,
        supportsFileSend,
        supportsQuickShare,
        supportsSteganography,
        supportsRtcDataChannel,
        clientId,
        operatingSystem,
        metadata,
    )

    override fun toString(): String = "PeerCapabilities[platform=$platform, appVersion=$appVersion, " +
        "supportsFileReceive=$supportsFileReceive, fileReceivePort=$fileReceivePort, supportsVoice=$supportsVoice, " +
        "supportsVideo=$supportsVideo, deviceName=$deviceName, protocolVersion=$protocolVersion]"

    enum class PlatformKind {
        DESKTOP,
        ANDROID,
        UNKNOWN,
        ;

        companion object {
            fun from(value: String?): PlatformKind = when (value?.trim()?.lowercase(Locale.ROOT)) {
                "desktop", "jvm", "javafx", "compose-desktop" -> DESKTOP
                "android" -> ANDROID
                else -> UNKNOWN
            }
        }
    }

    companion object {
        const val CURRENT_PROTOCOL_VERSION: Int = 1
        const val PLATFORM_DESKTOP: String = "desktop"
        const val PLATFORM_ANDROID: String = "android"
        const val PLATFORM_UNKNOWN: String = "unknown"
        private const val ENCODED_PREFIX = "caps:v1:"
        private const val FLAG_FILE_RECEIVE = 1
        private const val FLAG_FILE_SEND = 1 shl 1
        private const val FLAG_VOICE = 1 shl 2
        private const val FLAG_VIDEO = 1 shl 3
        private const val FLAG_QUICK_SHARE = 1 shl 4
        private const val FLAG_STEGANOGRAPHY = 1 shl 5
        private const val FLAG_RTC_DATA_CHANNEL = 1 shl 6

        @JvmStatic
        fun unknown(): PeerCapabilities = PeerCapabilities(PLATFORM_UNKNOWN, "", false, 0, false, false)

        @JvmStatic
        fun desktop(appVersion: String?, fileReceivePort: Int): PeerCapabilities = PeerCapabilities(
            PLATFORM_DESKTOP,
            appVersion,
            fileReceivePort in 1..65_535,
            fileReceivePort,
            supportsVoice = true,
            supportsVideo = true,
            deviceName = System.getProperty("user.name", "Desktop"),
            supportsFileSend = true,
            supportsQuickShare = true,
            supportsSteganography = true,
            supportsRtcDataChannel = true,
            operatingSystem = System.getProperty("os.name", ""),
        )

        @JvmStatic
        fun android(appVersion: String?, fileReceivePort: Int, deviceName: String? = "Android device"): PeerCapabilities = PeerCapabilities(
            PLATFORM_ANDROID,
            appVersion,
            fileReceivePort in 1..65_535,
            fileReceivePort,
            supportsVoice = false,
            supportsVideo = false,
            deviceName = deviceName,
            supportsFileSend = true,
        )

        @JvmStatic
        fun decode(value: String?): PeerCapabilities {
            val text = value?.trim().orEmpty()
            if (!text.startsWith(ENCODED_PREFIX)) {
                return unknown()
            }
            val body = text.removePrefix(ENCODED_PREFIX)
            if (!body.contains('&')) {
                return decodeCompact(body)
            }
            val fields = body
                .split('&')
                .filter { it.isNotBlank() }
                .mapNotNull { item ->
                    val index = item.indexOf('=')
                    if (index <= 0) null else item.substring(0, index) to decodeText(item.substring(index + 1))
                }
                .toMap()
            val metadata = fields
                .filterKeys { it.startsWith("meta.") }
                .mapKeys { it.key.removePrefix("meta.") }
            return PeerCapabilities(
                fields["platform"],
                fields["appVersion"],
                fields["supportsFileReceive"]?.toBooleanStrictOrNull() ?: false,
                fields["fileReceivePort"]?.toIntOrNull() ?: 0,
                fields["supportsVoice"]?.toBooleanStrictOrNull() ?: false,
                fields["supportsVideo"]?.toBooleanStrictOrNull() ?: false,
                fields["deviceName"],
                fields["protocolVersion"]?.toIntOrNull() ?: CURRENT_PROTOCOL_VERSION,
                fields["supportsFileSend"]?.toBooleanStrictOrNull() ?: false,
                fields["supportsQuickShare"]?.toBooleanStrictOrNull() ?: false,
                fields["supportsSteganography"]?.toBooleanStrictOrNull() ?: false,
                fields["supportsRtcDataChannel"]?.toBooleanStrictOrNull() ?: false,
                fields["clientId"],
                fields["operatingSystem"],
                metadata,
            )
        }

        private fun decodeCompact(body: String): PeerCapabilities {
            val fields = body.split('|')
            if (fields.size < 4) {
                return unknown()
            }
            val flags = fields.getOrNull(3)?.toIntOrNull() ?: 0
            return PeerCapabilities(
                platformFromToken(fields.getOrNull(0)),
                decodeText(fields.getOrNull(1).orEmpty()),
                flags and FLAG_FILE_RECEIVE != 0,
                fields.getOrNull(2)?.toIntOrNull() ?: 0,
                flags and FLAG_VOICE != 0,
                flags and FLAG_VIDEO != 0,
                decodeText(fields.getOrNull(4).orEmpty()),
                fields.getOrNull(5)?.toIntOrNull() ?: CURRENT_PROTOCOL_VERSION,
                flags and FLAG_FILE_SEND != 0,
                flags and FLAG_QUICK_SHARE != 0,
                flags and FLAG_STEGANOGRAPHY != 0,
                flags and FLAG_RTC_DATA_CHANNEL != 0,
            )
        }

        @JvmStatic
        fun isEncoded(value: String?): Boolean = value?.trim()?.startsWith(ENCODED_PREFIX) == true

        private fun normalizeText(value: String?): String = value?.trim().orEmpty().take(256)

        private fun normalizeKey(value: String): String = value.trim()
            .lowercase(Locale.ROOT)
            .filter { it.isLetterOrDigit() || it == '-' || it == '_' || it == '.' }
            .take(64)

        private fun encodeText(value: String): String = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

        private fun decodeText(value: String): String = runCatching {
            String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
        }.getOrDefault("")

        private fun platformToken(value: String): String = when (PlatformKind.from(value)) {
            PlatformKind.DESKTOP -> "d"
            PlatformKind.ANDROID -> "a"
            PlatformKind.UNKNOWN -> "u"
        }

        private fun platformFromToken(value: String?): String = when (value) {
            "d" -> PLATFORM_DESKTOP
            "a" -> PLATFORM_ANDROID
            else -> PLATFORM_UNKNOWN
        }
    }
}
