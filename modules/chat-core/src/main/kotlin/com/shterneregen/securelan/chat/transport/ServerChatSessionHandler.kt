package com.shterneregen.securelan.chat.transport

import com.shterneregen.securelan.chat.event.ChatErrorEvent
import com.shterneregen.securelan.chat.event.ChatUserLeftEvent
import com.shterneregen.securelan.chat.protocol.WireMessageType
import com.shterneregen.securelan.chat.service.ChatBroadcastService
import com.shterneregen.securelan.chat.service.ChatEventPublisher
import com.shterneregen.securelan.chat.service.NicknameRegistryService
import java.io.IOException

/**
 * Server-side handler for a single authenticated chat session.
 *
 * Runs synchronously on a server I/O thread (one instance per connected client) after the
 * secure handshake has been completed.  Reads incoming [com.shterneregen.securelan.chat.protocol.WireMessage]s
 * in a blocking loop and delegates them to [ChatBroadcastService]:
 *
 * | Received type | Action |
 * |---------------|--------|
 * | [WireMessageType.DISCONNECT] | Exit the loop gracefully. |
 * | [WireMessageType.CHAT] (non-blank payload) | Broadcast the message to all peers. |
 * | [WireMessageType.SIGNAL] (non-blank payload) | Route the signal to the addressed peer. |
 * | anything else | Ignored. |
 *
 * ## Cleanup (always performed in `finally`)
 * 1. Removes the client from [ChatBroadcastService].
 * 2. Unregisters the nickname from [NicknameRegistryService].
 * 3. Broadcasts a `USER_LEFT` event.
 * 4. Publishes a [ChatUserLeftEvent] to the server's [ChatEventPublisher].
 * 5. Closes [session].
 *
 * ## Error handling
 * [IOException]s are reported via [ChatEventPublisher] only when [reportSessionErrors] returns
 * `true` (i.e. while the server is still running).  This suppresses spurious error events that
 * occur because the server forcibly closed client sockets during [com.shterneregen.securelan.chat.service.impl.DefaultChatServerService.stop].
 *
 * @param session             The authenticated [ChatSocketSession] for this client.
 * @param nickname            The registered nickname for this client.
 * @param broadcastService    Handles fan-out messaging.
 * @param nicknameRegistry    Tracks active nicknames.
 * @param eventPublisher      Receives server-side lifecycle events.
 * @param reportSessionErrors Returns `true` while the server is running; used to suppress
 *                            expected errors during shutdown.  Defaults to always `true`.
 */
class ServerChatSessionHandler(
    private val session: ChatSocketSession,
    private val nickname: String,
    private val broadcastService: ChatBroadcastService,
    private val nicknameRegistry: NicknameRegistryService,
    private val eventPublisher: ChatEventPublisher,
    private val reportSessionErrors: () -> Boolean = { true },
) : Runnable {

    /**
     * Runs the session handler loop.
     *
     * Blocks until the client sends a [WireMessageType.DISCONNECT] frame, the remote end closes
     * the connection (EOF), or an [IOException] occurs.
     */
    override fun run() {
        try {
            while (true) {
                val message = session.readMessage() ?: break
                when {
                    message.type() == WireMessageType.DISCONNECT -> break

                    message.type() == WireMessageType.CHAT && message.payload().isNotBlank() ->
                        broadcastService.publishMessage(nickname, message.payload())

                    message.type() == WireMessageType.SIGNAL && message.payload().isNotBlank() ->
                        broadcastService.publishSignal(nickname, message.payload())
                }
            }
        } catch (e: IOException) {
            if (reportSessionErrors()) {
                eventPublisher.publish(ChatErrorEvent("Server session error for $nickname", e))
            }
        } finally {
            cleanup()
        }
    }

    /**
     * Unregisters the client from all shared services and closes the session socket.
     *
     * Always called, even when an [IOException] is thrown in the read loop.
     */
    private fun cleanup() {
        broadcastService.removeClient(nickname)
        nicknameRegistry.unregister(nickname)
        broadcastService.publishUserLeft(nickname)
        eventPublisher.publish(ChatUserLeftEvent(nickname))
        try {
            session.close()
        } catch (_: IOException) {
        }
    }
}
