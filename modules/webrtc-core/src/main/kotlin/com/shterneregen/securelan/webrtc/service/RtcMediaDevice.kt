package com.shterneregen.securelan.webrtc.service

import java.util.Objects

class RtcMediaDevice(id: String?, name: String?, private val defaultDevice: Boolean) {
    private val id: String = id?.trim() ?: ""
    private val name: String = if (name.isNullOrBlank()) "Unknown media device" else name.trim()

    fun id(): String = id
    fun name(): String = name
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
