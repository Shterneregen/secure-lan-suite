package com.shterneregen.securelan.common.net.transport

import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Factory for creating and establishing outbound client TCP [Socket] connections to a [TransportEndpoint].
 */
fun interface ClientSocketFactory {
    /**
     * Connects a new client [Socket] to the specified [endpoint].
     *
     * @param endpoint the target host and port to connect to.
     * @return an open and connected [Socket].
     * @throws IOException if a network error occurs while establishing the connection.
     */
    @Throws(IOException::class)
    fun connect(endpoint: TransportEndpoint): Socket

    companion object {
        /**
         * Default connection timeout in milliseconds.
         */
        const val DEFAULT_CONNECT_TIMEOUT_MILLIS: Int = 5_000

        /**
         * Returns a default [ClientSocketFactory] that connects standard [Socket]s using the given timeout.
         *
         * @param connectTimeoutMillis socket connection timeout in milliseconds (must be positive).
         * @return a [ClientSocketFactory] configured with the specified timeout.
         */
        @JvmStatic
        @JvmOverloads
        fun systemDefault(connectTimeoutMillis: Int = DEFAULT_CONNECT_TIMEOUT_MILLIS): ClientSocketFactory {
            require(connectTimeoutMillis > 0) { "connectTimeoutMillis must be positive" }
            return ClientSocketFactory { endpoint ->
                val socket = Socket()
                try {
                    socket.connect(InetSocketAddress(endpoint.host(), endpoint.port()), connectTimeoutMillis)
                    socket
                } catch (error: Throwable) {
                    SocketClose.closeQuietly(socket)
                    throw error
                }
            }
        }
    }
}
