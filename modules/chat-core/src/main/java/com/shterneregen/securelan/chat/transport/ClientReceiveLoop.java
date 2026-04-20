package com.shterneregen.securelan.chat.transport;

import com.shterneregen.securelan.chat.event.ChatDisconnectedEvent;
import com.shterneregen.securelan.chat.event.ChatErrorEvent;
import com.shterneregen.securelan.chat.event.ChatMessageReceivedEvent;
import com.shterneregen.securelan.chat.event.ChatSignalReceivedEvent;
import com.shterneregen.securelan.chat.protocol.WireMessage;
import com.shterneregen.securelan.chat.protocol.WireMessageType;
import com.shterneregen.securelan.chat.service.ChatEventPublisher;
import com.shterneregen.securelan.common.model.rtc.RtcSignalCodec;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientReceiveLoop implements Runnable {
    private final ChatSocketSession session;
    private final String nickname;
    private final AtomicBoolean connected;
    private final ChatEventPublisher eventPublisher;

    public ClientReceiveLoop(ChatSocketSession session, String nickname, AtomicBoolean connected, ChatEventPublisher eventPublisher) {
        this.session = session;
        this.nickname = nickname;
        this.connected = connected;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void run() {
        try {
            WireMessage message;
            while (connected.get() && (message = session.readMessage()) != null) {
                if (message.type() == WireMessageType.CHAT || message.type() == WireMessageType.SYSTEM) {
                    eventPublisher.publish(new ChatMessageReceivedEvent(message.sender(), message.payload()));
                } else if (message.type() == WireMessageType.SIGNAL) {
                    eventPublisher.publish(new ChatSignalReceivedEvent(RtcSignalCodec.deserialize(message.payload())));
                }
            }
        } catch (IOException e) {
            if (connected.get()) {
                eventPublisher.publish(new ChatErrorEvent("Connection lost", e));
            }
        } finally {
            if (connected.getAndSet(false)) {
                eventPublisher.publish(new ChatDisconnectedEvent(nickname, "Client disconnected"));
                try {
                    session.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
