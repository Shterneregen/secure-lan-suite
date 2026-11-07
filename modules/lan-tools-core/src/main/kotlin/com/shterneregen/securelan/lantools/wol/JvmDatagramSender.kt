package com.shterneregen.securelan.lantools.wol

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.StandardProtocolFamily
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import kotlin.time.Duration.Companion.milliseconds

/**
 * Nonblocking IPv4 UDP. Backpressure suspends, allowing the service deadline to cancel and close.
 *
 * A call binds the route's source address and owns one channel until completion. It performs no DNS,
 * receives no reply and makes no delivery claim. Short OS open/configure/bind calls run on IO;
 * cancellation cannot forcibly interrupt a stuck kernel call. Direct callers supply a deadline.
 */
class JvmDatagramSender internal constructor(
    private val dispatcher: CoroutineDispatcher,
    private val open: (WakeRoute) -> SendChannel,
) : DatagramSender {
    /** Creates a sender with the shared IO dispatcher and ordinary JVM datagram channels. */
    constructor() : this(Dispatchers.IO, ::openChannel)

    override suspend fun send(route: WakeRoute, payload: ByteArray) = withContext(dispatcher) {
        currentCoroutineContext().ensureActive()
        open(route).use { channel ->
            val target = InetSocketAddress(InetAddress.getByAddress(route.target.bytes()), route.port)
            while (true) {
                currentCoroutineContext().ensureActive()
                when (channel.send(payload, target)) {
                    payload.size -> break
                    // Zero means no datagram was submitted; retrying it does not duplicate a send.
                    0 -> delay(10.milliseconds)
                    else -> throw IOException("Incomplete UDP datagram")
                }
            }
        }
    }
}

/** Test seam for backpressure, failure and cleanup without a physical broadcast-capable adapter. */
internal interface SendChannel : AutoCloseable {
    /** Nonblocking write: full length or zero; partial lengths are rejected by the sender. */
    fun send(payload: ByteArray, target: InetSocketAddress): Int
}

private fun openChannel(route: WakeRoute): SendChannel {
    val channel = DatagramChannel.open(StandardProtocolFamily.INET)
    try {
        channel.configureBlocking(false)
        channel.setOption(StandardSocketOptions.SO_BROADCAST, true)
        // Numeric bytes avoid implicit DNS; an ephemeral source port needs no persistent listener.
        channel.bind(InetSocketAddress(InetAddress.getByAddress(route.network.binding.address.bytes()), 0))
        return object : SendChannel {
            override fun send(payload: ByteArray, target: InetSocketAddress): Int = channel.send(ByteBuffer.wrap(payload), target)
            override fun close() = channel.close()
        }
    } catch (error: Throwable) {
        // Setup failures happen before use() owns the channel, so this branch must close it itself.
        try { channel.close() } catch (closeError: Throwable) { error.addSuppressed(closeError) }
        throw error
    }
}
