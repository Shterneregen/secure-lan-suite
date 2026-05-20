package com.shterneregen.securelan.common.net.transport

import java.io.IOException
import java.net.ServerSocket

fun interface ServerSocketFactory {
    @Throws(IOException::class)
    fun open(port: Int): ServerSocket

    companion object {
        @JvmStatic
        fun systemDefault(): ServerSocketFactory = ServerSocketFactory { port -> ServerSocket(port) }
    }
}
