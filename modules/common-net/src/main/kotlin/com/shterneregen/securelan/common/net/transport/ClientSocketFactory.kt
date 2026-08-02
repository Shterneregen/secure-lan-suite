package com.shterneregen.securelan.common.net.transport

import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

fun interface ClientSocketFactory {
    @Throws(IOException::class)
    fun connect(endpoint: TransportEndpoint): Socket

    companion object {
        @JvmStatic
        fun systemDefault(): ClientSocketFactory = ClientSocketFactory { endpoint ->
            Socket().also { socket ->
                try {
                    socket.connect(InetSocketAddress(endpoint.host(), endpoint.port()), DEFAULT_CONNECT_TIMEOUT_MILLIS)
                } catch (error: IOException) {
                    socket.close()
                    throw error
                }
            }
        }

        private const val DEFAULT_CONNECT_TIMEOUT_MILLIS = 5_000
    }
}
