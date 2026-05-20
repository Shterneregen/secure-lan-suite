package com.shterneregen.securelan.filetransfer.quickshare

import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.Objects

class QuickShareCreateRequest(
    type: QuickShareType?,
    displayName: String?,
    file: Path?,
    text: String?,
    expiresAfter: Duration?,
    accessLimit: Int,
) {
    private val typeValue: QuickShareType
    private val displayNameValue: String
    private val fileValue: Path?
    private val textValue: String
    private val expiresAfterValue: Duration
    private val accessLimitValue: Int

    init {
        val requiredType = Objects.requireNonNull(type, "type must not be null")!!
        val requiredDisplayName = Objects.requireNonNull(displayName, "displayName must not be null")!!
        val requiredExpiresAfter = Objects.requireNonNull(expiresAfter, "expiresAfter must not be null")!!
        require(requiredDisplayName.isNotBlank()) { "displayName must not be blank" }
        require(!(requiredExpiresAfter.isZero || requiredExpiresAfter.isNegative)) { "expiresAfter must be positive" }
        require(accessLimit >= 1) { "accessLimit must be at least 1" }

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
        expiresAfterValue = requiredExpiresAfter
        accessLimitValue = accessLimit
    }

    fun type(): QuickShareType = typeValue

    fun displayName(): String = displayNameValue

    fun file(): Path? = fileValue

    fun text(): String = textValue

    fun expiresAfter(): Duration = expiresAfterValue

    fun accessLimit(): Int = accessLimitValue

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is QuickShareCreateRequest) return false
        return accessLimitValue == other.accessLimitValue &&
            typeValue == other.typeValue &&
            displayNameValue == other.displayNameValue &&
            fileValue == other.fileValue &&
            textValue == other.textValue &&
            expiresAfterValue == other.expiresAfterValue
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
        @JvmStatic
        fun file(file: Path?, displayName: String?, expiresAfter: Duration?, accessLimit: Int): QuickShareCreateRequest {
            val requiredFile = Objects.requireNonNull(file, "file must not be null")!!
            val resolvedName = if (displayName.isNullOrBlank()) requiredFile.fileName.toString() else displayName
            return QuickShareCreateRequest(QuickShareType.FILE, resolvedName, requiredFile, "", expiresAfter, accessLimit)
        }

        @JvmStatic
        fun text(text: String?, displayName: String?, expiresAfter: Duration?, accessLimit: Int): QuickShareCreateRequest {
            val resolvedName = if (displayName.isNullOrBlank()) "shared-text" else displayName
            return QuickShareCreateRequest(QuickShareType.TEXT, resolvedName, null, text, expiresAfter, accessLimit)
        }
    }
}
