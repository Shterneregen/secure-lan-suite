package com.shterneregen.securelan.chat.service

import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope

/**
 * Client-side service for connecting to, communicating with, and disconnecting from a chat server.
 *
 * ## Lifecycle
 * 1. Call [connect] to establish a TCP connection and perform the secure handshake.
 * 2. Use [sendMessage] and [sendSignal] to communicate while [isConnected] returns `true`.
 * 3. Call [disconnect] to leave the room gracefully.
 *
 * Events (connection established, messages received, errors, etc.) are delivered asynchronously
 * through the [ChatEventPublisher] provided at construction time.
 *
 * Implementations must be thread-safe.
 */
interface ChatClientService {
    /** Signing identity for file connections bound to authenticated room presence. */
    fun fileIdentity(): com.shterneregen.securelan.crypto.protocol.PeerIdentity? = null

    /**
     * Connects to the chat server described by [request] and performs the secure handshake.
     *
     * Returns `true` immediately if the client is already connected.
     * Returns `false` if the connection attempt fails or the handshake is rejected; in either
     * case a [com.shterneregen.securelan.chat.event.ChatErrorEvent] is published before returning.
     *
     * A [com.shterneregen.securelan.chat.event.ChatConnectedEvent] is published on success.
     *
     * @param request Connection parameters (host, port, nickname, password, capabilities).
     * @return `true` if the client is now connected, `false` otherwise.
     */
    fun connect(request: ChatClientConnectRequest): Boolean

    /**
     * Sends a graceful [com.shterneregen.securelan.chat.protocol.WireMessageType.DISCONNECT]
     * frame, closes the socket, and publishes a
     * [com.shterneregen.securelan.chat.event.ChatDisconnectedEvent].
     *
     * Safe to call even if the client is not currently connected.
     */
    fun disconnect()

    /**
     * Sends a chat message to the server (which then broadcasts it to all connected peers).
     *
     * Returns a typed rejection if disconnected or empty. WRITTEN means local socket write only;
     * there is no recipient delivery acknowledgement.
     * On success, a [com.shterneregen.securelan.chat.event.ChatMessageSentEvent] is published.
     *
     * @param text The message text to send.
     */
    fun sendMessage(text: String?): ChatSendOutcome

    /**
     * Sends a WebRTC signalling envelope to the server for routing to the addressed peer.
     *
     * Silently ignored if the client is not connected or [signal] is `null`.
     *
     * @param signal The [RtcSignalEnvelope] to forward.
     */
    fun sendSignal(signal: RtcSignalEnvelope?)

    /**
     * Returns `true` if the client currently has an active, authenticated session with the server.
     */
    fun isConnected(): Boolean
}
