package com.shterneregen.securelan.filetransfer.quickshare

import java.time.Instant
import java.util.Objects

class QuickShareSnapshot(
    id: String?,
    type: QuickShareType?,
    displayName: String?,
    fileName: String?,
    fileSize: Long,
    createdAt: Instant?,
    expiresAt: Instant?,
    accessLimit: Int,
    accessCount: Int,
    status: QuickShareStatus?,
    urls: List<String>?,
) {
    private val idValue: String = Objects.requireNonNull(id, "id must not be null")!!
    private val typeValue: QuickShareType = Objects.requireNonNull(type, "type must not be null")!!
    private val displayNameValue: String = Objects.requireNonNull(displayName, "displayName must not be null")!!
    private val fileNameValue: String = Objects.requireNonNull(fileName, "fileName must not be null")!!
    private val fileSizeValue: Long = fileSize
    private val createdAtValue: Instant = Objects.requireNonNull(createdAt, "createdAt must not be null")!!
    private val expiresAtValue: Instant = Objects.requireNonNull(expiresAt, "expiresAt must not be null")!!
    private val accessLimitValue: Int = accessLimit
    private val accessCountValue: Int = accessCount
    private val statusValue: QuickShareStatus = Objects.requireNonNull(status, "status must not be null")!!
    private val urlsValue: List<String>

    init {
        val requiredUrls = Objects.requireNonNull(urls, "urls must not be null")!!
        require(accessLimit >= 1) { "accessLimit must be at least 1" }
        require(accessCount >= 0) { "accessCount must not be negative" }
        urlsValue = requiredUrls.toList()
    }

    fun id(): String = idValue

    fun type(): QuickShareType = typeValue

    fun displayName(): String = displayNameValue

    fun fileName(): String = fileNameValue

    fun fileSize(): Long = fileSizeValue

    fun createdAt(): Instant = createdAtValue

    fun expiresAt(): Instant = expiresAtValue

    fun accessLimit(): Int = accessLimitValue

    fun accessCount(): Int = accessCountValue

    fun status(): QuickShareStatus = statusValue

    fun urls(): List<String> = urlsValue

    fun active(): Boolean = statusValue == QuickShareStatus.ACTIVE

    fun primaryUrl(): String = urlsValue.firstOrNull() ?: ""
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is QuickShareSnapshot) return false
        return fileSizeValue == other.fileSizeValue &&
            accessLimitValue == other.accessLimitValue &&
            accessCountValue == other.accessCountValue &&
            idValue == other.idValue &&
            typeValue == other.typeValue &&
            displayNameValue == other.displayNameValue &&
            fileNameValue == other.fileNameValue &&
            createdAtValue == other.createdAtValue &&
            expiresAtValue == other.expiresAtValue &&
            statusValue == other.statusValue &&
            urlsValue == other.urlsValue
    }

    override fun hashCode(): Int = Objects.hash(
        idValue,
        typeValue,
        displayNameValue,
        fileNameValue,
        fileSizeValue,
        createdAtValue,
        expiresAtValue,
        accessLimitValue,
        accessCountValue,
        statusValue,
        urlsValue,
    )

    override fun toString(): String =
        "QuickShareSnapshot[id=$idValue, type=$typeValue, displayName=$displayNameValue, fileName=$fileNameValue, " +
            "fileSize=$fileSizeValue, createdAt=$createdAtValue, expiresAt=$expiresAtValue, " +
            "accessLimit=$accessLimitValue, accessCount=$accessCountValue, status=$statusValue, urls=$urlsValue]"
}
