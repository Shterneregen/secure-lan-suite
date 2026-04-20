package com.shterneregen.securelan.chat.transport;

import com.shterneregen.securelan.chat.event.ChatErrorEvent;
import com.shterneregen.securelan.chat.event.ChatUserLeftEvent;
import com.shterneregen.securelan.chat.protocol.WireMessage;
import com.shterneregen.securelan.chat.protocol.WireMessageType;
import com.shterneregen.securelan.chat.service.ChatBroadcastService;
import com.shterneregen.securelan.chat.service.ChatEventPublisher;
import com.shterneregen.securelan.chat.service.NicknameRegistryService;

import java.io.IOException;

public class ServerChatSessionHandler implements Runnable {
    private final ChatSocketSession session;
    private final String nickname;
    private final ChatBroadcastService broadcastService;
    private final NicknameRegistryService nicknameRegistry;
    private final ChatEventPublisher eventPublisher;

    public ServerChatSessionHandler(
            ChatSocketSession session,
            String nickname,
            ChatBroadcastService broadcastService,
            NicknameRegistryService nicknameRegistry,
            ChatEventPublisher eventPublisher
    ) {
        this.session = session;
        this.nickname = nickname;
        this.broadcastService = broadcastService;
        this.nicknameRegistry = nicknameRegistry;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void run() {
        try {
            WireMessage message;
            while ((message = session.readMessage()) != null) {
                if (message.type() == WireMessageType.DISCONNECT) {
                    break;
                }
                if (message.type() == WireMessageType.CHAT && !message.payload().isBlank()) {
                    broadcastService.publishMessage(nickname, message.payload());
                } else if (message.type() == WireMessageType.SIGNAL && !message.payload().isBlank()) {
                    broadcastService.publishSignal(nickname, message.payload());
                }
            }
        } catch (IOException e) {
            eventPublisher.publish(new ChatErrorEvent("Server session error for " + nickname, e));
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        broadcastService.removeClient(nickname);
        nicknameRegistry.unregister(nickname);
        broadcastService.publishUserLeft(nickname);
        eventPublisher.publish(new ChatUserLeftEvent(nickname));
        try {
            session.close();
        } catch (IOException ignored) {
        }
    }
}
