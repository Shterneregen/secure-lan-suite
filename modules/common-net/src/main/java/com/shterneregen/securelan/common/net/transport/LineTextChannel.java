package com.shterneregen.securelan.common.net.transport;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class LineTextChannel implements Closeable {
    private final Socket socket;
    private final BufferedReader reader;
    private final BufferedWriter writer;

    public LineTextChannel(Socket socket) throws IOException {
        this.socket = Objects.requireNonNull(socket, "socket");
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    public String readLine() throws IOException {
        return reader.readLine();
    }

    public void writeLine(String line) throws IOException {
        Objects.requireNonNull(line, "line");
        synchronized (writer) {
            writer.write(line);
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
