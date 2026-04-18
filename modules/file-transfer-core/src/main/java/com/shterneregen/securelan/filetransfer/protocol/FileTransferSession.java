package com.shterneregen.securelan.filetransfer.protocol;

import com.shterneregen.securelan.crypto.service.AesGcmCryptoService;

import javax.crypto.SecretKey;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class FileTransferSession implements Closeable {
    private final Socket socket;
    private final DataInputStream input;
    private final DataOutputStream output;

    private volatile SecretKey transportKey;
    private volatile AesGcmCryptoService aesGcmCryptoService;

    public FileTransferSession(Socket socket) throws IOException {
        this.socket = socket;
        this.input = new DataInputStream(socket.getInputStream());
        this.output = new DataOutputStream(socket.getOutputStream());
    }

    public void writeUtf(String value) throws IOException {
        synchronized (output) {
            output.writeUTF(value);
            output.flush();
        }
    }

    public String readUtf() throws IOException {
        return input.readUTF();
    }

    public void writeBytes(byte[] bytes) throws IOException {
        synchronized (output) {
            output.writeInt(bytes.length);
            output.write(bytes);
            output.flush();
        }
    }

    public byte[] readBytes() throws IOException {
        int length = input.readInt();
        if (length < 0) {
            throw new IOException("Negative payload length");
        }
        byte[] bytes = input.readNBytes(length);
        if (bytes.length != length) {
            throw new IOException("Unexpected end of stream");
        }
        return bytes;
    }

    public void writeEncryptedText(String value) throws IOException {
        writeEncryptedBytes(value.getBytes(StandardCharsets.UTF_8));
    }

    public String readEncryptedText() throws IOException {
        return new String(readEncryptedBytes(), StandardCharsets.UTF_8);
    }

    public void writeEncryptedBytes(byte[] plainBytes) throws IOException {
        ensureSecure();
        writeBytes(aesGcmCryptoService.encrypt(plainBytes, transportKey));
    }

    public byte[] readEncryptedBytes() throws IOException {
        ensureSecure();
        return aesGcmCryptoService.decrypt(readBytes(), transportKey);
    }

    public void enableTransportEncryption(SecretKey secretKey, AesGcmCryptoService aesGcmCryptoService) {
        this.transportKey = Objects.requireNonNull(secretKey, "secretKey");
        this.aesGcmCryptoService = Objects.requireNonNull(aesGcmCryptoService, "aesGcmCryptoService");
    }

    public String remoteAddress() {
        return socket.getRemoteSocketAddress().toString();
    }

    private void ensureSecure() {
        if (transportKey == null || aesGcmCryptoService == null) {
            throw new IllegalStateException("Secure transport is not enabled");
        }
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
