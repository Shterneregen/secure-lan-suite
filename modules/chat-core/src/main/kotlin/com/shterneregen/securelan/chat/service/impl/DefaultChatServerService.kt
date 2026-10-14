package com.shterneregen.securelan.chat.service.impl

import com.shterneregen.securelan.chat.event.ChatErrorEvent
import com.shterneregen.securelan.chat.event.ChatUserJoinedEvent
import com.shterneregen.securelan.chat.service.ChatBroadcastService
import com.shterneregen.securelan.chat.service.ChatEventPublisher
import com.shterneregen.securelan.chat.service.ChatHistoryService
import com.shterneregen.securelan.chat.service.ChatServerConfig
import com.shterneregen.securelan.chat.service.ChatServerService
import com.shterneregen.securelan.chat.service.NicknameRegistryService
import com.shterneregen.securelan.chat.service.SecureHandshakeService
import com.shterneregen.securelan.chat.transport.ChatSocketSession
import com.shterneregen.securelan.chat.transport.ServerChatSessionHandler
import com.shterneregen.securelan.common.net.transport.SocketClose
import com.shterneregen.securelan.common.net.transport.TcpServer
import java.io.IOException
import java.net.Socket
import java.util.Objects
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Default production implementation of [ChatServerService].
 *
 * ## Architecture
 * - A [TcpServer] accepts incoming TCP connections on a dedicated thread pool and dispatches
 *   each connection to [handleConnection] on a separate thread.
 * - Each connection goes through [SimpleHandshakeService]; successfully authenticated clients
 *   are handed off to a [ServerChatSessionHandler] that blocks until the client disconnects.
 * - [InMemoryChatBroadcastService] fan-outs messages and signals to all connected clients.
 * - [InMemoryNicknameRegistryService] enforces case-insensitive nickname uniqueness.
 *
 * ## Thread safety
 * [running] is an [AtomicBoolean].  [activeSessions] is a concurrent set.  All shared state
 * is safe for concurrent access by [TcpServer]'s accept threads.
 *
 * @param eventPublisher Receives server-side lifecycle events (user joins, errors, etc.).
 * @throws NullPointerException if [eventPublisher] is `null`.
 */
class DefaultChatServerService(eventPublisher: ChatEventPublisher) : ChatServerService {

    private val eventPublisher: ChatEventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher")
    private val handshakeService: SecureHandshakeService = SimpleHandshakeService()
    private val nicknameRegistry: NicknameRegistryService = InMemoryNicknameRegistryService()
    private val historyService: ChatHistoryService = InMemoryChatHistoryService()
    private val broadcastService: ChatBroadcastService = InMemoryChatBroadcastService(historyService)
    private val activeSessions = ConcurrentHashMap.newKeySet<ChatSocketSession>()
    private val running = AtomicBoolean(false)
    private val tcpServer = TcpServer("chat-server")
    private val membershipLock = Any()

    private lateinit var config: ChatServerConfig

    /**
     * Starts the TCP server on [ChatServerConfig.port].
     *
     * If the server is already running, this call is a no-op.
     *
     * @param config Server configuration (port and session password).
     * @throws IllegalStateException if the TCP server fails to bind.
     */
    override fun start(config: ChatServerConfig) {
        if (!running.compareAndSet(false, true)) {
            return
        }
        try {
            this.config = config
            tcpServer.start(config.port, this::handleConnection, this::publishAcceptError)
        } catch (e: Exception) {
            if (e is IOException || e is RuntimeException) {
                running.set(false)
                throw IllegalStateException("Unable to start chat server", e)
            }
            throw e
        }
    }

    /**
     * Publishes an accept-loop error as a [ChatErrorEvent], but only while the server is
     * still running (errors during [stop] are intentional and are suppressed).
     */
    private fun publishAcceptError(message: String, cause: Throwable) {
        if (running.get()) {
            eventPublisher.publish(ChatErrorEvent(message, cause))
        }
    }

    /**
     * Handles a single accepted TCP connection:
     * 1. Wraps [socket] in a [ChatSocketSession].
     * 2. Performs the secure handshake.
     * 3. Syncs existing peers and broadcasts the join event.
     * 4. Runs [ServerChatSessionHandler] until the client disconnects.
     *
     * The session is always removed from [activeSessions] in the finally block.
     */
    private fun handleConnection(socket: Socket) {
        if (!running.get()) {
            SocketClose.closeQuietly(socket)
            return
        }
        var session: ChatSocketSession? = null
        try {
            session = ChatSocketSession(socket)
            activeSessions.add(session)
            // Re-check running after adding to activeSessions; stop() may have just fired.
            if (!running.get()) {
                session.close()
                return
            }
            session.setReadTimeout(HANDSHAKE_TIMEOUT_MILLIS)
            val response = handshakeService.performServerHandshake(session, config.sessionPassword, nicknameRegistry)
            if (!response.accepted()) {
                session.close()
                return
            }
            session.setReadTimeout(0)
            val nickname = response.nickname()
            synchronized(membershipLock) {
                broadcastService.syncPeers(session, nickname)
                broadcastService.addClient(nickname, session, response.capabilities())
                broadcastService.publishUserJoined(nickname, response.capabilities())
            }
            eventPublisher.publish(ChatUserJoinedEvent(nickname, session.remoteAddress(), response.capabilities(), serverSide = true))
            ServerChatSessionHandler(
                session = session,
                nickname = nickname,
                broadcastService = broadcastService,
                nicknameRegistry = nicknameRegistry,
                eventPublisher = eventPublisher,
                reportSessionErrors = running::get,
            ).run()
        } catch (e: Exception) {
            eventPublisher.publish(ChatErrorEvent("Error while handling client", e))
            SocketClose.closeQuietly(session)
        } finally {
            if (session != null) {
                activeSessions.remove(session)
            }
        }
    }

    /**
     * Stops the server, closes all active client sessions, and clears internal state.
     */
    override fun stop() {
        running.set(false)
        tcpServer.close()
        activeSessions.forEach { session ->
            try {
                session.close()
            } catch (_: IOException) {
            }
        }
        activeSessions.clear()
    }

    /** Returns `true` if the server is currently accepting connections. */
    override fun isRunning(): Boolean = running.get()

    /** Returns the number of clients that have completed the handshake and are connected. */
    override fun connectedUsers(): Int = nicknameRegistry.getActiveNicknames().size

    companion object {
        /** Read timeout (ms) applied to new connections during the handshake phase. */
        private const val HANDSHAKE_TIMEOUT_MILLIS = 10_000
    }
}
