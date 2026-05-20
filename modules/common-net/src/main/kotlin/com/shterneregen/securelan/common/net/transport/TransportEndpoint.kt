package com.shterneregen.securelan.common.net.transport

import java.net.InetSocketAddress
import java.util.Objects

class TransportEndpoint(host: String, private val port: Int) {
    private val host: String

    init {
        val trimmedHost = Objects.requireNonNull(host, "host").trim()
        require(trimmedHost.isNotBlank()) { "host must not be blank" }
        require(port in 1..65_535) { "port must be between 1 and 65535" }
        this.host = trimmedHost
    }

    fun host(): String = host

    fun port(): Int = port

    fun toSocketAddress(): InetSocketAddress = InetSocketAddress(host, port)

    override fun equals(other: Any?): Boolean =
        this === other || (other is TransportEndpoint && host == other.host && port == other.port)

    override fun hashCode(): Int = Objects.hash(host, port)

    override fun toString(): String = "TransportEndpoint[host=$host, port=$port]"

    companion object {
        @JvmStatic
        fun of(host: String, port: Int): TransportEndpoint = TransportEndpoint(host, port)
    }
}
