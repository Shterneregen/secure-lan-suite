package com.shterneregen.securelan.crypto.util

class CryptoExceptions private constructor() {
    companion object {
        @JvmStatic
        fun missing(name: String): IllegalArgumentException = IllegalArgumentException("$name must not be null")

        @JvmStatic
        fun empty(name: String): IllegalArgumentException = IllegalArgumentException("$name must not be empty")

        @JvmStatic
        fun failed(action: String, cause: Exception): IllegalStateException = IllegalStateException("Failed to $action", cause)
    }
}
