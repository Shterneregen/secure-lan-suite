package com.shterneregen.securelan.common.net.transport;

import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.FutureTask;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LineTextChannelTest {
    @Test
    void shouldExchangeUtf8Lines() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            FutureTask<String> serverTask = new FutureTask<>(() -> {
                try (Socket socket = serverSocket.accept(); LineTextChannel channel = new LineTextChannel(socket)) {
                    String received = channel.readLine();
                    channel.writeLine("reply: " + received);
                    return channel.remoteAddress();
                }
            });
            Thread serverThread = new Thread(serverTask, "line-text-channel-test-server");
            serverThread.start();

            try (Socket socket = new Socket("127.0.0.1", serverSocket.getLocalPort());
                 LineTextChannel channel = new LineTextChannel(socket)) {
                channel.writeLine("Привет SecureLanSuite");
                assertEquals("reply: Привет SecureLanSuite", channel.readLine());
            }

            String remoteAddress = serverTask.get();
            assertTrue(remoteAddress.contains("127.0.0.1") || remoteAddress.contains("localhost"));
        }
    }
}
