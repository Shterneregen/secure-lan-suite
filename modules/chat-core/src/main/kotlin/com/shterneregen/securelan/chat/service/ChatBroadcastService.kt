package com.shterneregen.securelan.chat.service

import com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities
import com.shterneregen.securelan.chat.transport.ChatSocketSession

/**
 * Server-side service that manages connected chat clients and distributes messages between them.
 *
 * An instance is created once per server session and is shared by all [ServerChatSessionHandler][com.shterneregen.securelan.chat.transport.ServerChatSessionHandler]
 * threads.  All methods must be thread-safe.
 */
interface ChatBroadcastService {

    /**
     * Registers a newly connected client so it can receive future broadcasts.
     *
     * @param nickname     The nickname of the joining client (already registered in [NicknameRegistryService]).
     * @param session      The active [ChatSocketSession] for the client.
     * @param capabilities The [PeerCapabilities] advertised by the client during the handshake.
     */
    fun addClient(nickname: String, session: ChatSocketSession, capabilities: PeerCapabilities)

    /**
     * Removes a client from the broadcast list.
     *
     * Called when a client disconnects (gracefully or due to an error).
     *
     * @param nickname The nickname of the client to remove.
     */
    fun removeClient(nickname: String)

    /**
     * Sends a [com.shterneregen.securelan.chat.protocol.WireMessageType.USER_JOINED] frame for
     * every currently connected peer to [session], so the new client can populate its user list.
     *
     * @param session         The session of the newly joined client.
     * @param excludeNickname The nickname to exclude from the sync (usually the joining client's
     *                        own nickname); `null` to include all clients.
     */
    fun syncPeers(session: ChatSocketSession, excludeNickname: String?)

    /**
     * Broadcasts a [com.shterneregen.securelan.chat.protocol.WireMessageType.USER_JOINED] event
     * and a [com.shterneregen.securelan.chat.protocol.WireMessageType.SYSTEM] announcement for
     * [nickname] to all connected clients (including the new client itself).
     *
     * Also appends the system line to the chat history.
     *
     * @param nickname     The nickname of the client that joined.
     * @param capabilities The [PeerCapabilities] of the joining client.
     */
    fun publishUserJoined(nickname: String, capabilities: PeerCapabilities)

    /**
     * Broadcasts a [com.shterneregen.securelan.chat.protocol.WireMessageType.USER_LEFT] event
     * and a [com.shterneregen.securelan.chat.protocol.WireMessageType.SYSTEM] announcement for
     * [nickname] to all remaining connected clients.
     *
     * Also appends the system line to the chat history.
     *
     * @param nickname The nickname of the client that left.
     */
    fun publishUserLeft(nickname: String)

    /**
     * Broadcasts a [com.shterneregen.securelan.chat.protocol.WireMessageType.CHAT] frame to all
     * connected clients and appends the message to the chat history.
     *
     * @param senderNickname The nickname of the client who sent the message.
     * @param text           The message text.
     */
    fun publishMessage(senderNickname: String, text: String)

    /**
     * Routes a WebRTC signalling message to the addressed peer.
     *
     * The [payload] must be a valid serialized [com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope].
     * The sender field in the envelope is overwritten with [senderNickname] to prevent spoofing.
     * If the payload cannot be parsed, the target peer is blank, or the target is not connected,
     * the call is silently ignored.
     *
     * @param senderNickname The authenticated nickname of the sending client.
     * @param payload        The raw serialized [com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope] string.
     */
    fun publishSignal(senderNickname: String, payload: String)
}
