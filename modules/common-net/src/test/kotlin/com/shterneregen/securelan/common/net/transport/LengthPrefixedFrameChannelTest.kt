package com.shterneregen.securelan.common.net.transport

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.DataOutputStream
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
            val serverTask = FutureTask {
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
                    assertNotNull(channel.remoteAddress())
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
            val serverTask = FutureTask {
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

    @Test
    fun shouldRejectNegativeFrameLength() {
        ServerSocket(0).use { serverSocket ->
            val serverTask = FutureTask {
                serverSocket.accept().use { socket ->
                    DataOutputStream(socket.getOutputStream()).use { out ->
                        out.writeInt(-1)
                        out.flush()
                    }
                }
            }
            Thread(serverTask, "frame-channel-negative-server").start()

            Socket("127.0.0.1", serverSocket.localPort).use { socket ->
                LengthPrefixedFrameChannel(socket).use { channel ->
                    assertThrows(IOException::class.java) { channel.readFrame() }
                }
            }
            serverTask.get()
        }
    }

    @Test
    fun shouldValidateMaxFrameSizeBytes() {
        ServerSocket(0).use { serverSocket ->
            val serverTask = FutureTask {
                serverSocket.accept().use { }
            }
            Thread(serverTask, "frame-channel-invalid-arg-server").start()

            Socket("127.0.0.1", serverSocket.localPort).use { socket ->
                assertThrows(IllegalArgumentException::class.java) {
                    LengthPrefixedFrameChannel(socket, 0)
                }
            }
            serverTask.get()
        }
    }
}
