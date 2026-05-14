package com.shterneregen.securelan.chat.transport;

import com.shterneregen.securelan.chat.protocol.WireMessage;
import com.shterneregen.securelan.common.net.transport.LineTextChannel;
import com.shterneregen.securelan.crypto.service.AesGcmCryptoService;

import javax.crypto.SecretKey;
import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

public class ChatSocketSession implements Closeable {
    private final LineTextChannel channel;

    private volatile SecretKey transportKey;
    private volatile AesGcmCryptoService aesGcmCryptoService;

    public ChatSocketSession(Socket socket) throws IOException {
        this.channel = new LineTextChannel(socket);
    }

    public WireMessage readMessage() throws IOException {
        String line = channel.readLine();
        if (line == null) {
            return null;
        }
        if (isSecure()) {
            byte[] encrypted = Base64.getDecoder().decode(line);
            byte[] decrypted = aesGcmCryptoService.decrypt(encrypted, transportKey);
            line = new String(decrypted, StandardCharsets.UTF_8);
        }
        return WireMessage.deserialize(line);
    }

    public void writeMessage(WireMessage message) throws IOException {
        String line = message.serialize();
        if (isSecure()) {
            byte[] encrypted = aesGcmCryptoService.encrypt(line.getBytes(StandardCharsets.UTF_8), transportKey);
            line = Base64.getEncoder().encodeToString(encrypted);
        }
        channel.writeLine(line);
    }

    public void enableTransportEncryption(SecretKey secretKey, AesGcmCryptoService aesGcmCryptoService) {
        this.transportKey = Objects.requireNonNull(secretKey, "secretKey");
        this.aesGcmCryptoService = Objects.requireNonNull(aesGcmCryptoService, "aesGcmCryptoService");
    }

    public boolean isSecure() {
        return transportKey != null && aesGcmCryptoService != null;
    }

    public String remoteAddress() {
        return channel.remoteAddress();
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}
