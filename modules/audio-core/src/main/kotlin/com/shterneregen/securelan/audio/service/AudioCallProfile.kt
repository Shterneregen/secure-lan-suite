package com.shterneregen.securelan.audio.service

@JvmRecord
data class AudioCallProfile(
    val sampleRateHz: Int,
    val channels: Int,
    val echoCancellation: Boolean,
    val noiseSuppression: Boolean,
)
