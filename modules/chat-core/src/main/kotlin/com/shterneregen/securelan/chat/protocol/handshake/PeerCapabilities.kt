package com.shterneregen.securelan.chat.protocol.handshake

import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Locale
import java.util.Objects

/**
 * Immutable value object that describes the features supported by a chat peer.
 *
 * Capabilities are exchanged during the handshake phase and are used by the UI and service layer
 * to decide which features to expose (file transfer, voice/video calls, quick-share, etc.).
 *
 * ### Encoding format
 * Capabilities are serialized to a compact single-line string by [encode] and deserialized by
 * [decode].  The format starts with the prefix `caps:v1:` and encodes fields as pipe-separated
 * tokens:
 * ```
 * caps:v1:<platform-token>|<appVersion-b64>|<fileReceivePort>|<flags>|<deviceName-b64>|<protocolVersion>|<identity-public-key>
 * ```
 * where `<flags>` is a bitmask of the `FLAG_*` constants.
 *
 * A legacy key-value format (`caps:v1:<key>=<value>&...`) is also supported by [decode] for
 * backward compatibility.
 *
 * ### Factory methods
 * Use the companion-object factory functions instead of the constructor directly:
 * - [unknown] — a sentinel with no known capabilities.
 * - [desktop] — pre-filled for desktop (JVM/Compose) clients.
 * - [android] — pre-filled for Android clients.
 *
 * @property platform             Normalized platform identifier (one of [PLATFORM_DESKTOP],
 *                                [PLATFORM_ANDROID], or [PLATFORM_UNKNOWN]).
 * @property appVersion           The application version string, truncated to 256 characters.
 * @property supportsFileReceive  Whether the peer can receive file transfers.
 * @property fileReceivePort      The TCP port on which the peer accepts incoming files,
 *                                or `0` if file receive is not supported.
 * @property supportsVoice        Whether the peer supports voice calls via WebRTC.
 * @property supportsVideo        Whether the peer supports video calls via WebRTC.
 * @property deviceName           A human-readable device name, truncated to 256 characters.
 * @property protocolVersion      The protocol version number; always ≥ 1.
 * @property supportsFileSend     Whether the peer can initiate file transfers.
 * @property supportsQuickShare   Whether the peer supports the quick-share feature.
 * @property supportsSteganography Whether the peer supports steganography features.
 * @property supportsRtcDataChannel Whether the peer supports WebRTC data channels.
 * @property clientId             An optional opaque client identifier, truncated to 256 characters.
 * @property operatingSystem      The operating system name (e.g. `"Windows 11"`), truncated to 256 characters.
 * @property metadata             An optional map of extension key-value pairs. Keys are
 *                                normalized (lower-case, alphanumeric/`-`/`_`/`.`, max 64 chars).
 */
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

    /** Returns the normalized platform identifier. */
    fun platform(): String = platform

    /** Returns the application version string. */
    fun appVersion(): String = appVersion

    /** Returns `true` if the peer can accept incoming file transfers. */
    fun supportsFileReceive(): Boolean = supportsFileReceive

    /** Returns the TCP port used for file receiving, or `0` if not supported. */
    fun fileReceivePort(): Int = fileReceivePort

    /** Returns `true` if the peer supports voice calls via WebRTC. */
    fun supportsVoice(): Boolean = supportsVoice

    /** Returns `true` if the peer supports video calls via WebRTC. */
    fun supportsVideo(): Boolean = supportsVideo

    /** Returns the human-readable device name. */
    fun deviceName(): String = deviceName

    /** Returns the protocol version (always ≥ 1). */
    fun protocolVersion(): Int = protocolVersion

    /** Returns `true` if the peer can initiate file transfers. */
    fun supportsFileSend(): Boolean = supportsFileSend

    /** Returns `true` if the peer supports the quick-share feature. */
    fun supportsQuickShare(): Boolean = supportsQuickShare

    /** Returns `true` if the peer supports steganography features. */
    fun supportsSteganography(): Boolean = supportsSteganography

    /** Returns `true` if the peer supports WebRTC data channels. */
    fun supportsRtcDataChannel(): Boolean = supportsRtcDataChannel

    /** Returns the opaque client identifier. */
    fun clientId(): String = clientId

    /** Returns the operating system name. */
    fun operatingSystem(): String = operatingSystem

    /** Returns the immutable extension metadata map. */
    fun metadata(): Map<String, String> = metadata

    /** Public signing identity certified by the current authenticated room presence. */
    fun identityKey(): String = metadata["identity-key"].orEmpty()

    fun withIdentity(publicKey: String): PeerCapabilities = PeerCapabilities(
        platform, appVersion, supportsFileReceive, fileReceivePort, supportsVoice, supportsVideo,
        deviceName, protocolVersion, supportsFileSend, supportsQuickShare, supportsSteganography,
        supportsRtcDataChannel, clientId, operatingSystem, metadata + ("identity-key" to publicKey),
    )

    /**
     * Returns a copy of this instance with the file-receiver settings updated.
     *
     * @param fileReceivePort The port on which the peer will accept incoming files.
     * @param enabled         Whether file receive should be enabled; defaults to `true`.
     * @return A new [PeerCapabilities] with the updated file-receive settings.
     */
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

    /**
     * Serializes this instance to the compact `caps:v1:` wire format.
     *
     * The result can be decoded by [decode].
     *
     * @return A non-null single-line capabilities string.
     */
    fun encode(): String {
        val flags = buildFlags()
        return ENCODED_PREFIX + listOf(
            platformToken(platform),
            encodeText(appVersion),
            fileReceivePort.toString(),
            flags.toString(),
            encodeText(deviceName),
            protocolVersion.toString(),
            identityKey(),
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

    /**
     * Returns the [PlatformKind] derived from the [platform] string.
     *
     * @return [PlatformKind.DESKTOP], [PlatformKind.ANDROID], or [PlatformKind.UNKNOWN].
     */
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

    /**
     * Enum that classifies the [platform] field into broad platform categories.
     */
    enum class PlatformKind {
        /** Desktop (JVM, Compose Desktop, legacy JavaFX, etc.) */
        DESKTOP,

        /** Android. */
        ANDROID,

        /** Unknown or unrecognized platform. */
        UNKNOWN,
        ;

        companion object {
            /**
             * Classifies a raw platform string into a [PlatformKind].
             *
             * @param value The raw platform string (case-insensitive).
             * @return The matching [PlatformKind], or [UNKNOWN] if unrecognized.
             */
            fun from(value: String?): PlatformKind = when (value?.trim()?.lowercase(Locale.ROOT)) {
                "desktop", "jvm", "javafx", "compose-desktop" -> DESKTOP
                "android" -> ANDROID
                else -> UNKNOWN
            }
        }
    }

    companion object {
        /** The current protocol version advertised by this build. */
        const val CURRENT_PROTOCOL_VERSION: Int = 2

        /** Platform token for desktop (JVM/Compose) clients. */
        const val PLATFORM_DESKTOP: String = "desktop"

        /** Platform token for Android clients. */
        const val PLATFORM_ANDROID: String = "android"

        /** Platform token used when the platform is not known. */
        const val PLATFORM_UNKNOWN: String = "unknown"

        private const val ENCODED_PREFIX = "caps:v1:"
        private const val FLAG_FILE_RECEIVE = 1
        private const val FLAG_FILE_SEND = 1 shl 1
        private const val FLAG_VOICE = 1 shl 2
        private const val FLAG_VIDEO = 1 shl 3
        private const val FLAG_QUICK_SHARE = 1 shl 4
        private const val FLAG_STEGANOGRAPHY = 1 shl 5
        private const val FLAG_RTC_DATA_CHANNEL = 1 shl 6

        /**
         * Creates a [PeerCapabilities] sentinel indicating that the peer's capabilities are not
         * known (e.g. for peers using an older protocol version).
         *
         * @return A [PeerCapabilities] with [PLATFORM_UNKNOWN] and all features disabled.
         */
        @JvmStatic
        fun unknown(): PeerCapabilities = PeerCapabilities(PLATFORM_UNKNOWN, "", false, 0, false, false)

        /**
         * Creates a [PeerCapabilities] instance pre-filled for a desktop (JVM/Compose) client.
         *
         * Voice, video, quick-share, steganography, and RTC data-channel are all enabled.
         * The device name and OS are read from system properties.
         *
         * @param appVersion      The application version string.
         * @param fileReceivePort The TCP port for incoming file transfers; `0` disables file receive.
         * @return A fully configured desktop [PeerCapabilities].
         */
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

        /**
         * Creates a [PeerCapabilities] instance pre-filled for an Android client.
         *
         * Voice and video are disabled by default on Android (hardware/permission constraints).
         *
         * @param appVersion      The application version string.
         * @param fileReceivePort The TCP port for incoming file transfers; `0` disables file receive.
         * @param deviceName      A human-readable device name; defaults to `"Android device"`.
         * @return A fully configured Android [PeerCapabilities].
         */
        @JvmStatic
        fun android(
            appVersion: String?,
            fileReceivePort: Int,
            deviceName: String? = "Android device",
        ): PeerCapabilities = PeerCapabilities(
            PLATFORM_ANDROID,
            appVersion,
            fileReceivePort in 1..65_535,
            fileReceivePort,
            supportsVoice = false,
            supportsVideo = false,
            deviceName = deviceName,
            supportsFileSend = true,
        )

        /**
         * Decodes a capabilities string produced by [encode] back into a [PeerCapabilities].
         *
         * Returns [unknown] if [value] is `null`, blank, or does not start with the expected prefix.
         *
         * Both the compact pipe-delimited format and the legacy key-value format are accepted.
         *
         * @param value The encoded capabilities string.
         * @return The decoded [PeerCapabilities], or [unknown] on parse failure.
         */
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

        /**
         * Decodes the compact pipe-delimited capabilities format.
         *
         * Returns [unknown] if [body] contains fewer than 4 pipe-separated tokens.
         */
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
                metadata = mapOf("identity-key" to fields.getOrNull(6).orEmpty()).filterValues { it.isNotEmpty() },
            )
        }

        /**
         * Returns `true` if [value] looks like a capabilities string produced by [encode].
         *
         * @param value The string to test.
         */
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
