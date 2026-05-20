package com.shterneregen.securelan.common.net.transport

import java.io.IOException
import java.net.Socket

fun interface ClientSocketFactory {
    @Throws(IOException::class)
    fun connect(endpoint: TransportEndpoint): Socket

    companion object {
        @JvmStatic
        fun systemDefault(): ClientSocketFactory = ClientSocketFactory { endpoint ->
            Socket(endpoint.host(), endpoint.port())
        }
    }
}
