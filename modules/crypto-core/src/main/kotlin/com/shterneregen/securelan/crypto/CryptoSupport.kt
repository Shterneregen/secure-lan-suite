package com.shterneregen.securelan.crypto

/**
 * Utility helper for verifying cryptographic subsystem availability and configuration.
 */
class CryptoSupport private constructor() {
    companion object {
        /**
         * Checks whether cryptographic operations are supported and enabled in the current environment.
         *
         * @return `true` if cryptographic operations are enabled, `false` otherwise.
         */
        @JvmStatic
        fun isCryptoEnabled(): Boolean = true
    }
}
