package com.shterneregen.securelan.filetransfer.protocol;

import com.shterneregen.securelan.crypto.service.AesGcmCryptoService;
import com.shterneregen.securelan.common.net.transport.LengthPrefixedFrameChannel;

import javax.crypto.SecretKey;
import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class FileTransferSession implements Closeable {
    private final LengthPrefixedFrameChannel channel;

    private volatile SecretKey transportKey;
    private volatile AesGcmCryptoService aesGcmCryptoService;

    public FileTransferSession(Socket socket) throws IOException {
        this.channel = new LengthPrefixedFrameChannel(socket);
    }

    public void writeUtf(String value) throws IOException {
        channel.writeUtf(value);
    }

    public String readUtf() throws IOException {
        return channel.readUtf();
    }

    public void writeBytes(byte[] bytes) throws IOException {
        channel.writeFrame(bytes);
    }

    public byte[] readBytes() throws IOException {
        return channel.readFrame();
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
        return channel.remoteAddress();
    }

    private void ensureSecure() {
        if (transportKey == null || aesGcmCryptoService == null) {
            throw new IllegalStateException("Secure transport is not enabled");
        }
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}
