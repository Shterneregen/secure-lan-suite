package com.shterneregen.securelan.filetransfer.quickshare

import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.Objects

/**
 * Request to create a new temporary Quick Share item on the local HTTP server.
 *
 * Use the static factory methods [QuickShareCreateRequest.file] or [QuickShareCreateRequest.text]
 * to construct valid requests for files or text snippets.
 *
 * @param type Share payload type ([QuickShareType.FILE] or [QuickShareType.TEXT]).
 * @param displayName Human-readable label for the share (must not be blank).
 * @param file Regular readable filesystem file (required if [type] is [QuickShareType.FILE]).
 * @param text Non-blank text string (required if [type] is [QuickShareType.TEXT]).
 * @param expiresAfter Optional positive duration after which the share automatically expires.
 * @param accessLimit Optional positive maximum access count before the share automatically terminates.
 */
class QuickShareCreateRequest(
    type: QuickShareType?,
    displayName: String?,
    file: Path?,
    text: String?,
    expiresAfter: Duration?,
    accessLimit: Int?,
) {
    private val typeValue: QuickShareType
    private val displayNameValue: String
    private val fileValue: Path?
    private val textValue: String
    private val expiresAfterValue: Duration?
    private val accessLimitValue: Int?

    init {
        val requiredType = Objects.requireNonNull(type, "type must not be null")!!
        val requiredDisplayName = Objects.requireNonNull(displayName, "displayName must not be null")!!
        require(requiredDisplayName.isNotBlank()) { "displayName must not be blank" }
        require(expiresAfter == null || !(expiresAfter.isZero || expiresAfter.isNegative)) {
            "expiresAfter must be positive when present"
        }
        require(accessLimit == null || accessLimit >= 1) { "accessLimit must be at least 1 when present" }

        val normalizedFile: Path?
        val normalizedText: String
        if (requiredType == QuickShareType.FILE) {
            val requiredFile = Objects.requireNonNull(file, "file must not be null for file shares")!!
            require(Files.isRegularFile(requiredFile) && Files.isReadable(requiredFile)) {
                "file must be a readable regular file"
            }
            normalizedFile = requiredFile
            normalizedText = ""
        } else {
            val requiredText = Objects.requireNonNull(text, "text must not be null for text shares")!!
            require(requiredText.isNotBlank()) { "text must not be blank" }
            normalizedFile = null
            normalizedText = requiredText
        }

        typeValue = requiredType
        displayNameValue = requiredDisplayName
        fileValue = normalizedFile
        textValue = normalizedText
        expiresAfterValue = expiresAfter
        accessLimitValue = accessLimit
    }

    /**
     * Payload type of the request.
     */
    fun type(): QuickShareType = typeValue

    /**
     * Human-readable display label.
     */
    fun displayName(): String = displayNameValue

    /**
     * Source filesystem path if this is a file share, or `null` for text shares.
     */
    fun file(): Path? = fileValue

    /**
     * Shared text content if this is a text share, or empty string for file shares.
     */
    fun text(): String = textValue

    /**
     * Configured duration until expiration, or `null` if unlimited.
     */
    fun expiresAfter(): Duration? = expiresAfterValue

    /**
     * Configured maximum access limit, or `null` if unlimited.
     */
    fun accessLimit(): Int? = accessLimitValue

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is QuickShareCreateRequest) return false
        return typeValue == other.typeValue &&
            displayNameValue == other.displayNameValue &&
            fileValue == other.fileValue &&
            textValue == other.textValue &&
            expiresAfterValue == other.expiresAfterValue &&
            accessLimitValue == other.accessLimitValue
    }

    override fun hashCode(): Int = Objects.hash(
        typeValue,
        displayNameValue,
        fileValue,
        textValue,
        expiresAfterValue,
        accessLimitValue,
    )

    override fun toString(): String =
        "QuickShareCreateRequest[type=$typeValue, displayName=$displayNameValue, file=$fileValue, " +
            "text=$textValue, expiresAfter=$expiresAfterValue, accessLimit=$accessLimitValue]"

    companion object {
        /**
         * Creates a [QuickShareCreateRequest] for a filesystem file.
         *
         * @param file Regular readable filesystem file to share.
         * @param displayName Optional display name. Defaults to the filename if null or blank.
         * @param expiresAfter Optional time-to-live duration.
         * @param accessLimit Optional maximum allowed downloads.
         * @return New configured [QuickShareCreateRequest].
         */
        @JvmStatic
        fun file(file: Path?, displayName: String?, expiresAfter: Duration?, accessLimit: Int?): QuickShareCreateRequest {
            val requiredFile = Objects.requireNonNull(file, "file must not be null")!!
            val fallbackName = requiredFile.fileName?.toString()?.takeIf { it.isNotBlank() } ?: "shared-file"
            val resolvedName = if (displayName.isNullOrBlank()) fallbackName else displayName
            return QuickShareCreateRequest(QuickShareType.FILE, resolvedName, requiredFile, "", expiresAfter, accessLimit)
        }

        /**
         * Creates a [QuickShareCreateRequest] for a text snippet.
         *
         * @param text Non-blank text to share.
         * @param displayName Optional display name. Defaults to "shared-text" if null or blank.
         * @param expiresAfter Optional time-to-live duration.
         * @param accessLimit Optional maximum allowed accesses.
         * @return New configured [QuickShareCreateRequest].
         */
        @JvmStatic
        fun text(text: String?, displayName: String?, expiresAfter: Duration?, accessLimit: Int?): QuickShareCreateRequest {
            val resolvedName = if (displayName.isNullOrBlank()) "shared-text" else displayName
            return QuickShareCreateRequest(QuickShareType.TEXT, resolvedName, null, text, expiresAfter, accessLimit)
        }
    }
}
