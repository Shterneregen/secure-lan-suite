package com.shterneregen.securelan.stego

/**
 * General runtime exception thrown when steganographic operations fail,
 * such as when processing invalid carrier images, encountering unsupported payload versions,
 * or dealing with corrupted hidden data.
 */
open class StegoException : RuntimeException {
    /**
     * Constructs a new [StegoException] with the specified detail message.
     *
     * @param message The detail message explaining the reason for the exception.
     */
    constructor(message: String) : super(message)

    /**
     * Constructs a new [StegoException] with the specified detail message and cause.
     *
     * @param message The detail message explaining the reason for the exception.
     * @param cause The underlying cause of the exception.
     */
    constructor(message: String, cause: Throwable) : super(message, cause)

    /**
     * Constructs a new [StegoException] with the specified cause.
     *
     * @param cause The underlying cause of the exception.
     */
    constructor(cause: Throwable) : super(cause)
}
