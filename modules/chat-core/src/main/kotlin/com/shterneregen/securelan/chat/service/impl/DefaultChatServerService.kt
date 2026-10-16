package com.shterneregen.securelan.chat.service.impl

import com.shterneregen.securelan.chat.event.ChatErrorEvent
import com.shterneregen.securelan.chat.event.ChatUserJoinedEvent
import com.shterneregen.securelan.chat.event.ChatUserLeftEvent
import com.shterneregen.securelan.chat.service.*
import com.shterneregen.securelan.chat.transport.ChatSocketSession
import com.shterneregen.securelan.chat.transport.ServerChatSessionHandler
import com.shterneregen.securelan.common.net.transport.SocketClose
import com.shterneregen.securelan.common.net.transport.TcpServer
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

/** A run owns its registrations and broadcasts; old workers cannot mutate a rehosted room. */
class DefaultChatServerService(private val eventPublisher: ChatEventPublisher) : ChatServerService {
    private class Run(val config: ChatServerConfig) {
        val tcp = TcpServer("chat-server")
        val registry = InMemoryNicknameRegistryService()
        val broadcast = InMemoryChatBroadcastService(InMemoryChatHistoryService())
        val sessions = ConcurrentHashMap.newKeySet<ChatSocketSession>()
        val membership = Any()
        @Volatile var active = true
    }
    private val lifecycle = Any()
    @Volatile private var current: Run? = null
    private val handshake = SimpleHandshakeService()

    override fun start(config: ChatServerConfig) {
        synchronized(lifecycle) {
            if (current != null) return
            val run = Run(config)
            current = run
            try {
                run.tcp.start(config.port, { handleConnection(run, it) }, { message, error ->
                    if (active(run)) eventPublisher.publish(ChatErrorEvent(message, error))
                })
            } catch (error: Exception) {
                run.active = false
                current = null
                run.tcp.close()
                throw IllegalStateException("Unable to start chat server", error)
            }
        }
    }

    private fun active(run: Run) = run.active && current === run

    private fun handleConnection(run: Run, socket: Socket) {
        if (!active(run)) { SocketClose.closeQuietly(socket); return }
        var session: ChatSocketSession? = null
        var registered: String? = null
        var joined = false
        // Lease covers registration inside the handshake, including failures before handler creation.
        val lease = object : NicknameRegistryService by run.registry {
            override fun register(nickname: String?): Boolean = synchronized(run.membership) {
                if (!active(run) || !run.registry.register(nickname)) false
                else { registered = nickname; true }
            }
            override fun unregister(nickname: String?) = synchronized(run.membership) {
                if (registered != null) { run.registry.unregister(registered); registered = null }
            }
        }
        try {
            val connection = ChatSocketSession(socket)
            session = connection
            run.sessions.add(connection)
            if (!active(run)) return
            connection.setReadTimeout(10_000)
            val response = handshake.performServerHandshake(connection, run.config.sessionPassword, lease)
            if (!response.accepted()) return
            connection.setReadTimeout(0)
            val nickname = response.nickname()
            synchronized(run.membership) {
                if (!active(run)) return
                run.broadcast.syncPeers(connection, nickname)
                run.broadcast.addClient(nickname, connection, response.capabilities())
                joined = true
                run.broadcast.publishUserJoined(nickname, response.capabilities())
                eventPublisher.publish(ChatUserJoinedEvent(nickname, connection.remoteAddress(), response.capabilities(), serverSide = true))
            }
            ServerChatSessionHandler(connection, nickname, run.broadcast, lease, eventPublisher,
                { active(run) }, { }).run()
        } catch (error: Exception) {
            if (active(run)) eventPublisher.publish(ChatErrorEvent("Error while handling client", error))
        } finally {
            try {
                synchronized(run.membership) {
                    val nickname = registered
                    if (nickname != null) {
                        // Join, snapshot and leave enqueue into the same per-recipient serial outboxes.
                        try {
                            if (joined) {
                                run.broadcast.removeClient(nickname)
                                run.broadcast.publishUserLeft(nickname)
                                if (active(run)) eventPublisher.publish(ChatUserLeftEvent(nickname, serverSide = true))
                            }
                        } finally { lease.unregister(nickname) }
                    }
                }
            } finally {
                session?.let { run.sessions.remove(it); SocketClose.closeQuietly(it) }
            }
        }
    }

    override fun stop() {
        synchronized(lifecycle) {
            val run = current ?: return
            current = null
            run.active = false
            run.tcp.close()
            run.sessions.forEach(SocketClose::closeQuietly)
            run.sessions.clear()
        }
    }
    override fun isRunning(): Boolean = current != null
    override fun connectedUsers(): Int = current?.registry?.getActiveNicknames()?.size ?: 0
}
