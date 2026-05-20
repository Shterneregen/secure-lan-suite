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

class DefaultChatServerService(eventPublisher: ChatEventPublisher) : ChatServerService {
    private val eventPublisher: ChatEventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher")
    private val handshakeService: SecureHandshakeService = SimpleHandshakeService()
    private val nicknameRegistry: NicknameRegistryService = InMemoryNicknameRegistryService()
    private val historyService: ChatHistoryService = InMemoryChatHistoryService()
    private val broadcastService: ChatBroadcastService = InMemoryChatBroadcastService(historyService)
    private val activeSessions = ConcurrentHashMap.newKeySet<ChatSocketSession>()
    private val running = AtomicBoolean(false)
    private val tcpServer = TcpServer("chat-server")

    private lateinit var config: ChatServerConfig

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

    private fun publishAcceptError(message: String, cause: Throwable) {
        if (running.get()) {
            eventPublisher.publish(ChatErrorEvent(message, cause))
        }
    }

    private fun handleConnection(socket: Socket) {
        if (!running.get()) {
            SocketClose.closeQuietly(socket)
            return
        }
        var session: ChatSocketSession? = null
        try {
            session = ChatSocketSession(socket)
            activeSessions.add(session)
            if (!running.get()) {
                session.close()
                return
            }
            val response = handshakeService.performServerHandshake(session, config.sessionPassword, nicknameRegistry)
            if (!response.accepted()) {
                session.close()
                return
            }
            val nickname = response.nickname()
            broadcastService.syncPeers(session, nickname)
            broadcastService.addClient(nickname, session)
            broadcastService.publishUserJoined(nickname)
            eventPublisher.publish(ChatUserJoinedEvent(nickname, session.remoteAddress()))
            ServerChatSessionHandler(session, nickname, broadcastService, nicknameRegistry, eventPublisher).run()
        } catch (e: IOException) {
            eventPublisher.publish(ChatErrorEvent("Error while handling client", e))
            SocketClose.closeQuietly(session)
        } finally {
            if (session != null) {
                activeSessions.remove(session)
            }
        }
    }

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

    override fun isRunning(): Boolean = running.get()

    override fun connectedUsers(): Int = nicknameRegistry.getActiveNicknames().size
}
