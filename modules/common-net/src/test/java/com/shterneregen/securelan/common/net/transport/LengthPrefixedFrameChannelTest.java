package com.shterneregen.securelan.common.net.transport;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.FutureTask;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LengthPrefixedFrameChannelTest {
    @Test
    void shouldExchangeUtfAndBinaryFrames() throws Exception {
        byte[] payload = "frame payload".getBytes(StandardCharsets.UTF_8);
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            FutureTask<byte[]> serverTask = new FutureTask<>(() -> {
                try (Socket socket = serverSocket.accept(); LengthPrefixedFrameChannel channel = new LengthPrefixedFrameChannel(socket)) {
                    assertEquals("metadata", channel.readUtf());
                    byte[] received = channel.readFrame();
                    channel.writeUtf("ack");
                    channel.writeFrame(received);
                    return received;
                }
            });
            Thread serverThread = new Thread(serverTask, "frame-channel-test-server");
            serverThread.start();

            try (Socket socket = new Socket("127.0.0.1", serverSocket.getLocalPort());
                 LengthPrefixedFrameChannel channel = new LengthPrefixedFrameChannel(socket)) {
                channel.writeUtf("metadata");
                channel.writeFrame(payload);
                assertEquals("ack", channel.readUtf());
                assertArrayEquals(payload, channel.readFrame());
            }

            assertArrayEquals(payload, serverTask.get());
        }
    }

    @Test
    void shouldRejectOversizedFrame() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            FutureTask<Void> serverTask = new FutureTask<>(() -> {
                try (Socket socket = serverSocket.accept(); LengthPrefixedFrameChannel ignored = new LengthPrefixedFrameChannel(socket, 4)) {
                    return null;
                }
            });
            Thread serverThread = new Thread(serverTask, "frame-channel-size-test-server");
            serverThread.start();

            try (Socket socket = new Socket("127.0.0.1", serverSocket.getLocalPort());
                 LengthPrefixedFrameChannel channel = new LengthPrefixedFrameChannel(socket, 4)) {
                assertThrows(IOException.class, () -> channel.writeFrame(new byte[5]));
            }
            serverTask.get();
        }
    }
}
