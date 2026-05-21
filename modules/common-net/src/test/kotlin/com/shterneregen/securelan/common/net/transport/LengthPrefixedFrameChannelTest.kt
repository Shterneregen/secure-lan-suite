package com.shterneregen.securelan.common.net.transport

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.FutureTask

class LengthPrefixedFrameChannelTest {
    @Test
    fun shouldExchangeUtfAndBinaryFrames() {
        val payload = "frame payload".toByteArray(StandardCharsets.UTF_8)
        ServerSocket(0).use { serverSocket ->
            val serverTask = FutureTask<ByteArray> {
                serverSocket.accept().use { socket ->
                    LengthPrefixedFrameChannel(socket).use { channel ->
                        assertEquals("metadata", channel.readUtf())
                        val received = channel.readFrame()
                        channel.writeUtf("ack")
                        channel.writeFrame(received)
                        received
                    }
                }
            }
            Thread(serverTask, "frame-channel-test-server").start()

            Socket("127.0.0.1", serverSocket.localPort).use { socket ->
                LengthPrefixedFrameChannel(socket).use { channel ->
                    channel.writeUtf("metadata")
                    channel.writeFrame(payload)
                    assertEquals("ack", channel.readUtf())
                    assertArrayEquals(payload, channel.readFrame())
                }
            }

            assertArrayEquals(payload, serverTask.get())
        }
    }

    @Test
    fun shouldRejectOversizedFrame() {
        ServerSocket(0).use { serverSocket ->
            val serverTask = FutureTask<Unit> {
                serverSocket.accept().use { socket ->
                    LengthPrefixedFrameChannel(socket, 4).use { }
                }
            }
            Thread(serverTask, "frame-channel-size-test-server").start()

            Socket("127.0.0.1", serverSocket.localPort).use { socket ->
                LengthPrefixedFrameChannel(socket, 4).use { channel ->
                    assertThrows(IOException::class.java) { channel.writeFrame(ByteArray(5)) }
                }
            }
            serverTask.get()
        }
    }
}
