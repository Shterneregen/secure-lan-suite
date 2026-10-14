package com.shterneregen.securelan.chat.transport

import com.shterneregen.securelan.chat.event.ChatDisconnectedEvent
import com.shterneregen.securelan.chat.event.ChatDisconnectReasons
import com.shterneregen.securelan.chat.event.ChatErrorEvent
import com.shterneregen.securelan.chat.event.ChatMessageReceivedEvent
import com.shterneregen.securelan.chat.event.ChatSignalReceivedEvent
import com.shterneregen.securelan.chat.event.ChatUserJoinedEvent
import com.shterneregen.securelan.chat.event.ChatUserLeftEvent
import com.shterneregen.securelan.chat.protocol.WireMessageType
import com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities
import com.shterneregen.securelan.chat.service.ChatEventPublisher
import com.shterneregen.securelan.common.model.rtc.RtcSignalCodec
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Background receive loop for the **client side** of a chat session.
 *
 * Reads [com.shterneregen.securelan.chat.protocol.WireMessage]s from [session] in a tight loop
 * and translates them into [com.shterneregen.securelan.chat.event.ChatCoreEvent]s that are
 * forwarded to [eventPublisher].
 *
 * ## Message handling
 * | Wire type      | Published event |
 * |----------------|-----------------|
 * | CHAT / SYSTEM  | [ChatMessageReceivedEvent] |
 * | USER_JOINED    | [ChatUserJoinedEvent] (with decoded [PeerCapabilities]) |
 * | USER_LEFT      | [ChatUserLeftEvent] |
 * | SIGNAL         | [ChatSignalReceivedEvent] (signal deserialized; invalid payloads are skipped) |
 * | other          | ignored |
 *
 * ## Disconnect handling
 * - **EOF** (server closed): publishes [ChatDisconnectedEvent] with reason [ChatDisconnectReasons.REMOTE_HOST_CLOSED].
 * - **IOException while connected**: publishes [ChatErrorEvent] + [ChatDisconnectedEvent] with reason
 *   [ChatDisconnectReasons.CONNECTION_LOST].
 * - **IOException after `connected` is cleared** (local disconnect in progress): error is suppressed.
 *
 * The [connected] flag is set to `false` in the `finally` block before the disconnect event is
 * published, preventing duplicate events when [com.shterneregen.securelan.chat.service.impl.DefaultChatClientService.disconnect]
 * races with an unexpected disconnection.
 *
 * @param session         The [ChatSocketSession] to read from.
 * @param nickname        The local user's nickname; included in the [ChatDisconnectedEvent].
 * @param connected       Shared flag used to signal the loop to stop and to detect whether a
 *                        disconnection was expected or unexpected.
 * @param eventPublisher  Receives all produced events.
 */
class ClientReceiveLoop(
    private val session: ChatSocketSession,
    private val nickname: String?,
    private val connected: AtomicBoolean,
    private val eventPublisher: ChatEventPublisher,
) : Runnable {

    /**
     * Runs the receive loop until [connected] becomes `false` or the remote end closes the
     * connection.
     */
    override fun run() {
        var disconnectReason = ChatDisconnectReasons.REMOTE_HOST_CLOSED
        try {
            while (connected.get()) {
                val message = session.readMessage() ?: break
                when (message.type()) {
                    WireMessageType.CHAT,
                    WireMessageType.SYSTEM,
                    -> eventPublisher.publish(ChatMessageReceivedEvent(message.sender(), message.payload(), message.type() == WireMessageType.SYSTEM))

                    WireMessageType.USER_JOINED ->
                        eventPublisher.publish(
                            ChatUserJoinedEvent(message.sender(), "", PeerCapabilities.decode(message.payload())),
                        )

                    WireMessageType.USER_LEFT ->
                        eventPublisher.publish(ChatUserLeftEvent(message.sender()))

                    WireMessageType.SIGNAL -> {
                        // Invalid signal payloads (e.g. malformed JSON) are silently ignored.
                        val signal = runCatching { RtcSignalCodec.deserialize(message.payload()) }.getOrNull()
                        if (signal != null) {
                            eventPublisher.publish(ChatSignalReceivedEvent(signal))
                        }
                    }

                    else -> Unit
                }
            }
        } catch (e: IOException) {
            if (connected.get()) {
                disconnectReason = ChatDisconnectReasons.CONNECTION_LOST
                eventPublisher.publish(ChatErrorEvent("Connection lost", e))
            }
        } finally {
            if (connected.getAndSet(false)) {
                eventPublisher.publish(ChatDisconnectedEvent(nickname, disconnectReason))
                try {
                    session.close()
                } catch (_: IOException) {
                }
            }
        }
    }
}
