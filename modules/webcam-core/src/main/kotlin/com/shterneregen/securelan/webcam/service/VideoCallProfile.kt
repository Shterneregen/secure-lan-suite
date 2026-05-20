package com.shterneregen.securelan.webcam.service

@JvmRecord
data class VideoCallProfile(
    val width: Int,
    val height: Int,
    val framesPerSecond: Int,
    val screenShareReady: Boolean,
)
