package com.shterneregen.securelan.common.net.transport;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Objects;

public final class LengthPrefixedFrameChannel implements Closeable {
    public static final int DEFAULT_MAX_FRAME_SIZE_BYTES = 128 * 1024 * 1024;

    private final Socket socket;
    private final DataInputStream input;
    private final DataOutputStream output;
    private final int maxFrameSizeBytes;

    public LengthPrefixedFrameChannel(Socket socket) throws IOException {
        this(socket, DEFAULT_MAX_FRAME_SIZE_BYTES);
    }

    public LengthPrefixedFrameChannel(Socket socket, int maxFrameSizeBytes) throws IOException {
        this.socket = Objects.requireNonNull(socket, "socket");
        if (maxFrameSizeBytes < 1) {
            throw new IllegalArgumentException("maxFrameSizeBytes must be positive");
        }
        this.maxFrameSizeBytes = maxFrameSizeBytes;
        this.input = new DataInputStream(socket.getInputStream());
        this.output = new DataOutputStream(socket.getOutputStream());
    }

    public void writeUtf(String value) throws IOException {
        Objects.requireNonNull(value, "value");
        synchronized (output) {
            output.writeUTF(value);
            output.flush();
        }
    }

    public String readUtf() throws IOException {
        return input.readUTF();
    }

    public void writeFrame(byte[] bytes) throws IOException {
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length > maxFrameSizeBytes) {
            throw new IOException("Frame exceeds maximum size: " + bytes.length + " > " + maxFrameSizeBytes);
        }
        synchronized (output) {
            output.writeInt(bytes.length);
            output.write(bytes);
            output.flush();
        }
    }

    public byte[] readFrame() throws IOException {
        int length = input.readInt();
        if (length < 0) {
            throw new IOException("Negative frame length");
        }
        if (length > maxFrameSizeBytes) {
            throw new IOException("Frame exceeds maximum size: " + length + " > " + maxFrameSizeBytes);
        }
        byte[] bytes = input.readNBytes(length);
        if (bytes.length != length) {
            throw new IOException("Unexpected end of stream");
        }
        return bytes;
    }

    public String remoteAddress() {
        return socket.getRemoteSocketAddress().toString();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
