package com.shterneregen.securelan.androidclient.lantools.network

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.lantools.diagnostics.TcpConnector
import com.shterneregen.securelan.lantools.wol.DatagramSender
import com.shterneregen.securelan.lantools.wol.WakeRoute
import kotlinx.coroutines.*
import java.io.IOException
import java.net.BindException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.channels.SocketChannel

/** Binds only this operation's unconnected socket. Numeric source and Android Network are both checked. */
class AndroidTcpConnector(private val platform: AndroidLanPlatform, private val io: CoroutineDispatcher = Dispatchers.IO) : TcpConnector {
    override suspend fun connect(address: IpLiteral, port: Int, source: IpLiteral?) = withContext(io) {
        currentCoroutineContext().ensureActive()
        SocketChannel.open().use { channel ->
            if (source != null) {
                val binding = platform.links().map { it.binding }.singleOrNull { it.address == source } ?: throw BindException("Source unavailable")
                val route = platform.route(binding) ?: throw BindException("Network changed")
                route.bind(channel.socket())
                channel.bind(InetSocketAddress(InetAddress.getByAddress(source.bytes()), 0))
            }
            channel.configureBlocking(false)
            if (!channel.connect(InetSocketAddress(InetAddress.getByAddress(address.bytes()), port))) {
                while (!channel.finishConnect()) { currentCoroutineContext().ensureActive(); delay(10) }
            }
            currentCoroutineContext().ensureActive()
        }
    }
}

/** One nonblocking datagram. Shared core owns validation, magic packet construction and deadline. */
class AndroidDatagramSender(private val platform: AndroidLanPlatform, private val io: CoroutineDispatcher = Dispatchers.IO) : DatagramSender {
    override suspend fun send(route: WakeRoute, payload: ByteArray) = withContext(io) {
        currentCoroutineContext().ensureActive()
        val binding = platform.route(route.network.binding) ?: throw BindException("Network changed")
        DatagramChannel.open().use { channel ->
            binding.bind(channel.socket())
            channel.socket().broadcast = true
            channel.bind(InetSocketAddress(InetAddress.getByAddress(route.network.binding.address.bytes()), 0))
            channel.configureBlocking(false)
            val target = InetSocketAddress(InetAddress.getByAddress(route.target.bytes()), route.port)
            while (true) {
                currentCoroutineContext().ensureActive()
                when (channel.send(ByteBuffer.wrap(payload), target)) {
                    payload.size -> break
                    0 -> delay(10)
                    else -> throw IOException("Incomplete UDP datagram")
                }
            }
        }
    }
}
