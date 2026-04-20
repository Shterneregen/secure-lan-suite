package com.shterneregen.securelan.chat.service.impl;

import com.shterneregen.securelan.chat.event.ChatConnectedEvent;
import com.shterneregen.securelan.chat.event.ChatDisconnectedEvent;
import com.shterneregen.securelan.chat.event.ChatErrorEvent;
import com.shterneregen.securelan.chat.event.ChatMessageSentEvent;
import com.shterneregen.securelan.chat.protocol.WireMessage;
import com.shterneregen.securelan.chat.protocol.WireMessageType;
import com.shterneregen.securelan.chat.protocol.handshake.HandshakeRequest;
import com.shterneregen.securelan.chat.protocol.handshake.HandshakeResponse;
import com.shterneregen.securelan.chat.service.ChatClientConnectRequest;
import com.shterneregen.securelan.chat.service.ChatClientService;
import com.shterneregen.securelan.chat.service.ChatEventPublisher;
import com.shterneregen.securelan.chat.service.SecureHandshakeService;
import com.shterneregen.securelan.chat.transport.ChatSocketSession;
import com.shterneregen.securelan.chat.transport.ClientReceiveLoop;
import com.shterneregen.securelan.common.model.rtc.RtcSignalCodec;
import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultChatClientService implements ChatClientService {
    private final ChatEventPublisher eventPublisher;
    private final SecureHandshakeService handshakeService;
    private final AtomicBoolean connected = new AtomicBoolean(false);

    private ChatSocketSession session;
    private Thread receiverThread;
    private String nickname;

    public DefaultChatClientService(ChatEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.handshakeService = new SimpleHandshakeService();
    }

    @Override
    public boolean connect(ChatClientConnectRequest request) {
        if (connected.get()) {
            return true;
        }
        try {
            session = new ChatSocketSession(new Socket(request.host(), request.port()));
            HandshakeResponse response = handshakeService.performClientHandshake(
                    session,
                    new HandshakeRequest(request.nickname(), request.sessionPassword())
            );
            if (!response.accepted()) {
                eventPublisher.publish(new ChatErrorEvent(response.reason(), null));
                disconnect();
                return false;
            }
            nickname = response.nickname();
            connected.set(true);
            eventPublisher.publish(new ChatConnectedEvent(nickname, session.remoteAddress()));
            receiverThread = new Thread(new ClientReceiveLoop(session, nickname, connected, eventPublisher), "chat-client-receive-loop");
            receiverThread.start();
            return true;
        } catch (IOException e) {
            eventPublisher.publish(new ChatErrorEvent("Unable to connect to chat server", e));
            disconnect();
            return false;
        }
    }

    @Override
    public void disconnect() {
        boolean wasConnected = connected.getAndSet(false);
        if (session != null) {
            try {
                session.writeMessage(new WireMessage(WireMessageType.DISCONNECT, nickname == null ? "" : nickname, ""));
            } catch (IOException ignored) {
            }
            try {
                session.close();
            } catch (IOException ignored) {
            }
            session = null;
        }
        if (wasConnected || nickname != null) {
            eventPublisher.publish(new ChatDisconnectedEvent(nickname == null ? "" : nickname, "Client disconnected"));
        }
    }

    @Override
    public void sendMessage(String text) {
        if (!connected.get() || text == null || text.isBlank() || session == null) {
            return;
        }
        try {
            session.writeMessage(new WireMessage(WireMessageType.CHAT, nickname, text));
            eventPublisher.publish(new ChatMessageSentEvent(nickname, text));
        } catch (IOException e) {
            eventPublisher.publish(new ChatErrorEvent("Unable to send message", e));
        }
    }

    @Override
    public void sendSignal(RtcSignalEnvelope signal) {
        if (!connected.get() || signal == null || session == null) {
            return;
        }
        try {
            RtcSignalEnvelope outboundSignal = signal.withSender(nickname == null ? signal.fromPeer() : nickname);
            session.writeMessage(new WireMessage(WireMessageType.SIGNAL, outboundSignal.fromPeer(), RtcSignalCodec.serialize(outboundSignal)));
        } catch (IOException e) {
            eventPublisher.publish(new ChatErrorEvent("Unable to send realtime signal", e));
        }
    }

    @Override
    public boolean isConnected() {
        return connected.get();
    }
}
