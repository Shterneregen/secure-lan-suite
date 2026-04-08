package com.shterneregen.securelan.chat.transport;

import com.shterneregen.securelan.chat.protocol.WireMessage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ChatSocketSession implements Closeable {
    private final Socket socket;
    private final BufferedReader reader;
    private final BufferedWriter writer;

    public ChatSocketSession(Socket socket) throws IOException {
        this.socket = socket;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    public WireMessage readMessage() throws IOException {
        String line = reader.readLine();
        if (line == null) {
            return null;
        }
        return WireMessage.deserialize(line);
    }

    public void writeMessage(WireMessage message) throws IOException {
        synchronized (writer) {
            writer.write(message.serialize());
            writer.newLine();
            writer.flush();
        }
    }

    public String remoteAddress() {
        return socket.getRemoteSocketAddress().toString();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
