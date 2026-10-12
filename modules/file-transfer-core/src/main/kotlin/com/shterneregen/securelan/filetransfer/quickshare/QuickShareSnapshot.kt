package com.shterneregen.securelan.filetransfer.quickshare

import java.time.Instant
import java.util.Objects

/**
 * Immutable snapshot of a Quick Share item's state at a point in time.
 *
 * Provides access to metadata, URLs, expiration time, download/access count,
 * and current lifecycle status.
 *
 * @param id Unique identifier of the share.
 * @param type Payload type ([QuickShareType.FILE] or [QuickShareType.TEXT]).
 * @param displayName Human-readable label for the share.
 * @param fileName Name of the underlying file (or empty for text shares).
 * @param fileSize Size of the shared payload in bytes.
 * @param createdAt Creation timestamp.
 * @param expiresAt Optional expiration timestamp, or `null` if unlimited.
 * @param accessLimit Optional maximum allowed accesses, or `null` if unlimited.
 * @param accessCount Total successful accesses recorded so far.
 * @param status Current lifecycle status of the share.
 * @param urls List of HTTP URLs where the share can be accessed across available network interfaces.
 */
class QuickShareSnapshot(
    id: String?,
    type: QuickShareType?,
    displayName: String?,
    fileName: String?,
    fileSize: Long,
    createdAt: Instant?,
    expiresAt: Instant?,
    accessLimit: Int?,
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
    private val expiresAtValue: Instant? = expiresAt
    private val accessLimitValue: Int? = accessLimit
    private val accessCountValue: Int = accessCount
    private val statusValue: QuickShareStatus = Objects.requireNonNull(status, "status must not be null")!!
    private val urlsValue: List<String>

    init {
        val requiredUrls = Objects.requireNonNull(urls, "urls must not be null")!!
        require(accessLimit == null || accessLimit >= 1) { "accessLimit must be at least 1 when present" }
        require(accessCount >= 0) { "accessCount must not be negative" }
        urlsValue = requiredUrls.toList()
    }

    /**
     * Unique identifier of this share.
     */
    fun id(): String = idValue

    /**
     * Payload type of this share.
     */
    fun type(): QuickShareType = typeValue

    /**
     * Human-readable display name.
     */
    fun displayName(): String = displayNameValue

    /**
     * Filename of the shared file, or empty string for text shares.
     */
    fun fileName(): String = fileNameValue

    /**
     * Size of the payload in bytes.
     */
    fun fileSize(): Long = fileSizeValue

    /**
     * Timestamp when the share was created.
     */
    fun createdAt(): Instant = createdAtValue

    /**
     * Expiration timestamp, or `null` if the share does not expire automatically.
     */
    fun expiresAt(): Instant? = expiresAtValue

    /**
     * Configured access limit, or `null` if access is unlimited.
     */
    fun accessLimit(): Int? = accessLimitValue

    /**
     * Number of times this share has been accessed.
     */
    fun accessCount(): Int = accessCountValue

    /**
     * Current status of the share.
     */
    fun status(): QuickShareStatus = statusValue

    /**
     * All advertised HTTP URLs where this share can be reached.
     */
    fun urls(): List<String> = urlsValue

    /**
     * Returns `true` if the share is currently [QuickShareStatus.ACTIVE].
     */
    fun active(): Boolean = statusValue == QuickShareStatus.ACTIVE

    /**
     * Returns the primary HTTP access URL, or empty string if no URLs are advertised.
     */
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
