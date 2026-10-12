package com.shterneregen.securelan.webrtc.service

import java.util.Objects

/**
 * Value object representing an enumerated hardware media device (microphone, speaker, or camera).
 *
 * @param id platform hardware descriptor or unique device identifier.
 * @param name human-friendly display name of the device.
 * @param defaultDevice `true` if this device is marked as the default system peripheral.
 */
class RtcMediaDevice(id: String?, name: String?, private val defaultDevice: Boolean) {
    private val id: String = id?.trim() ?: ""
    private val name: String = if (name.isNullOrBlank()) "Unknown media device" else name.trim()

    /** Returns the platform hardware identifier or descriptor string. */
    fun id(): String = id

    /** Returns the human-readable device name. */
    fun name(): String = name

    /** Returns `true` if this is the default system peripheral. */
    fun defaultDevice(): Boolean = defaultDevice

    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is RtcMediaDevice &&
                id == other.id &&
                name == other.name &&
                defaultDevice == other.defaultDevice)

    override fun hashCode(): Int = Objects.hash(id, name, defaultDevice)

    override fun toString(): String = "RtcMediaDevice[id=$id, name=$name, defaultDevice=$defaultDevice]"
}
