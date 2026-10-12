package com.shterneregen.securelan.crypto.util

/**
 * Utility factory for constructing standard exceptions used throughout the cryptography module.
 */
class CryptoExceptions private constructor() {
    companion object {
        /**
         * Creates an [IllegalArgumentException] indicating that a required argument or parameter was null.
         *
         * @param name The name of the missing argument or parameter.
         * @return An [IllegalArgumentException] with a descriptive message.
         */
        @JvmStatic
        fun missing(name: String): IllegalArgumentException = IllegalArgumentException("$name must not be null")

        /**
         * Creates an [IllegalArgumentException] indicating that a parameter (such as a byte array or string) was empty.
         *
         * @param name The name of the empty parameter.
         * @return An [IllegalArgumentException] with a descriptive message.
         */
        @JvmStatic
        fun empty(name: String): IllegalArgumentException = IllegalArgumentException("$name must not be empty")

        /**
         * Creates an [IllegalStateException] representing an unexpected failure during a cryptographic operation.
         *
         * @param action Description of the action that failed (e.g. "encrypt AES-GCM payload").
         * @param cause The underlying exception that triggered the failure.
         * @return An [IllegalStateException] encapsulating the failure and cause.
         */
        @JvmStatic
        fun failed(action: String, cause: Exception): IllegalStateException = IllegalStateException("Failed to $action", cause)
    }
}
