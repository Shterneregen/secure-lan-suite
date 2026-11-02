package com.shterneregen.securelan.lantools.diagnostics

import com.shterneregen.securelan.common.net.address.IpLiteral
import kotlinx.coroutines.*
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel

/** Nonblocking connect with cooperative backpressure. No application data or credentials are sent. */
class JvmTcpConnector internal constructor(
    private val dispatcher: CoroutineDispatcher,
    private val open: (IpLiteral?) -> ConnectChannel,
) : TcpConnector {
    constructor() : this(Dispatchers.IO, ::openChannel)

    /** Caller supplies a deadline; use [DefaultNetworkDiagnosticsService] for validated requests. */
    override suspend fun connect(address: IpLiteral, port: Int, source: IpLiteral?) = withContext(dispatcher) {
        currentCoroutineContext().ensureActive()
        open(source).use { channel ->
            val target = InetSocketAddress(InetAddress.getByAddress(address.bytes()), port)
            if (!channel.connect(target)) {
                while (true) {
                    currentCoroutineContext().ensureActive()
                    if (channel.finishConnect()) break
                    delay(10)
                }
            }
        }
    }
}

/** Narrow seam for stalled connect/cancel/cleanup tests without relying on network black holes. */
internal interface ConnectChannel : AutoCloseable {
    fun connect(target: InetSocketAddress): Boolean
    fun finishConnect(): Boolean
}

private fun openChannel(source: IpLiteral?): ConnectChannel {
    val channel = SocketChannel.open()
    try {
        channel.configureBlocking(false)
        source?.let { channel.bind(InetSocketAddress(InetAddress.getByAddress(it.bytes()), 0)) }
        return object : ConnectChannel {
            override fun connect(target: InetSocketAddress): Boolean = channel.connect(target)
            override fun finishConnect(): Boolean = channel.finishConnect()
            override fun close() = channel.close()
        }
    } catch (error: Throwable) {
        // Setup fails before use() owns the channel.
        try { channel.close() } catch (closeError: Throwable) { error.addSuppressed(closeError) }
        throw error
    }
}
