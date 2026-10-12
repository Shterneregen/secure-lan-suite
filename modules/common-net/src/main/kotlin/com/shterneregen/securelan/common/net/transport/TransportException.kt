package com.shterneregen.securelan.common.net.transport

/**
 * General runtime exception thrown when transport-level network operations fail.
 */
open class TransportException : RuntimeException {
    /**
     * Constructs a new [TransportException] with the specified detail message.
     *
     * @param message the detail message.
     */
    constructor(message: String) : super(message)

    /**
     * Constructs a new [TransportException] with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause the underlying cause.
     */
    constructor(message: String, cause: Throwable) : super(message, cause)

    /**
     * Constructs a new [TransportException] with the specified cause.
     *
     * @param cause the underlying cause.
     */
    constructor(cause: Throwable) : super(cause)
}
