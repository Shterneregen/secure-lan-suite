package com.shterneregen.securelan.common.net.transport

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.FutureTask

class LineTextChannelTest {
    @Test
    fun shouldExchangeUtf8Lines() {
        ServerSocket(0).use { serverSocket ->
            val serverTask = FutureTask<String> {
                serverSocket.accept().use { socket ->
                    LineTextChannel(socket).use { channel ->
                        val received = channel.readLine()
                        channel.writeLine("reply: $received")
                        channel.remoteAddress()
                    }
                }
            }
            Thread(serverTask, "line-text-channel-test-server").start()

            Socket("127.0.0.1", serverSocket.localPort).use { socket ->
                LineTextChannel(socket).use { channel ->
                    channel.writeLine("Привет SecureLanSuite")
                    assertEquals("reply: Привет SecureLanSuite", channel.readLine())
                }
            }

            val remoteAddress = serverTask.get()
            assertTrue(remoteAddress.contains("127.0.0.1") || remoteAddress.contains("localhost"))
        }
    }
}
