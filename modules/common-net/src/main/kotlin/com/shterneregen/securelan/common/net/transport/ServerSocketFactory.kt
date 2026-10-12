package com.shterneregen.securelan.common.net.transport

import java.io.IOException
import java.net.ServerSocket

/**
 * Factory for creating bound [ServerSocket] instances for inbound TCP connections.
 */
fun interface ServerSocketFactory {
    /**
     * Opens and binds a new [ServerSocket] on the specified [port].
     *
     * @param port the TCP port to bind to (0 for an ephemeral port, or 1..65535).
     * @return a bound [ServerSocket] ready to accept connections.
     * @throws IOException if the socket cannot be opened or bound.
     */
    @Throws(IOException::class)
    fun open(port: Int): ServerSocket

    companion object {
        /**
         * Returns the default system [ServerSocketFactory] using standard Java [ServerSocket].
         *
         * @return a standard [ServerSocketFactory].
         */
        @JvmStatic
        fun systemDefault(): ServerSocketFactory = ServerSocketFactory { port -> ServerSocket(port) }
    }
}
