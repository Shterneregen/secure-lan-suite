package com.shterneregen.securelan.common.model

/** Module responsible for an application event. */
enum class ModuleType {
    /** Chat and messaging. */
    CHAT,
    /** File transfers. */
    FILE_TRANSFER,
    /** Audio communication. */
    AUDIO,
    /** Webcam and video capture. */
    WEBCAM,
    /** Steganography. */
    STEGO,
    /** Cryptographic operations. */
    CRYPTO,
    /** Application-wide services. */
    SYSTEM,
}
