package com.shterneregen.securelan.common.net.transport

import java.net.InetSocketAddress
import java.util.Objects

/**
 * Represents an immutable remote or local transport endpoint defined by a host name/address and a TCP/UDP port.
 *
 * @param host the host name or IP address (must not be blank).
 * @param port the network port number (must be in range 1..65535).
 */
class TransportEndpoint(host: String, private val port: Int) {
    private val host: String

    init {
        val trimmedHost = host.trim()
        require(trimmedHost.isNotBlank()) { "host must not be blank" }
        require(port in 1..65_535) { "port must be between 1 and 65535" }
        this.host = trimmedHost
    }

    /**
     * Returns the host name or IP address string.
     */
    fun host(): String = host

    /**
     * Returns the network port number.
     */
    fun port(): Int = port

    /**
     * Converts this endpoint to an unresolved [InetSocketAddress].
     */
    fun toSocketAddress(): InetSocketAddress = InetSocketAddress(host, port)

    override fun equals(other: Any?): Boolean =
        this === other || (other is TransportEndpoint && host == other.host && port == other.port)

    override fun hashCode(): Int = Objects.hash(host, port)

    override fun toString(): String = "TransportEndpoint[host=$host, port=$port]"

    companion object {
        /**
         * Creates a new [TransportEndpoint] for the given [host] and [port].
         *
         * @param host the target host name or IP address.
         * @param port the target port number.
         * @return a new [TransportEndpoint] instance.
         */
        @JvmStatic
        fun of(host: String, port: Int): TransportEndpoint = TransportEndpoint(host, port)
    }
}
