package com.shterneregen.securelan.desktop.compose

import com.shterneregen.securelan.chat.discovery.PeerDiscoveryConfig
import com.shterneregen.securelan.chat.discovery.PeerDiscoveryListener
import com.shterneregen.securelan.chat.discovery.PeerDiscoveryService
import com.shterneregen.securelan.chat.service.ChatClientConnectRequest
import com.shterneregen.securelan.chat.service.ChatClientService
import com.shterneregen.securelan.chat.service.ChatServerConfig
import com.shterneregen.securelan.chat.service.ChatServerService
import com.shterneregen.securelan.filetransfer.service.FileTransferServerConfig
import com.shterneregen.securelan.filetransfer.service.FileTransferServerService
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicLong

/** Owns acquisition and rollback of a hosted room. Blocking work runs on the caller's IO worker. */
internal class RoomSessionController(
    private val server: ChatServerService,
    private val client: ChatClientService,
    private val files: FileTransferServerService,
    private val discovery: PeerDiscoveryService,
) {
    private val generation = AtomicLong()

    fun invalidate() { generation.incrementAndGet() }
    fun operationGeneration(): Long = generation.get()

    fun join(request: ChatClientConnectRequest, file: FileTransferServerConfig, listen: PeerDiscoveryConfig,
        previousDiscovery: PeerDiscoveryConfig?, listener: PeerDiscoveryListener, token: Long = generation.get()): Boolean {
        val rollback = ArrayDeque<() -> Unit>()
        fun checkCurrent() { if (generation.get() != token) throw CancellationException("Room connection cancelled") }
        try {
            checkCurrent()
            rollback.addFirst { client.disconnect() }
            if (!client.connect(request)) { rollback.forEach { runCatching(it) }; return false }
            checkCurrent()
            if (!server.isRunning() || !files.isRunning()) {
                rollback.addFirst { files.stop() }
                files.start(file)
            }
            checkCurrent()
            if (!server.isRunning()) {
                rollback.addFirst {
                    discovery.stop()
                    if (previousDiscovery != null && generation.get() == token) discovery.start(previousDiscovery, listener)
                }
                discovery.start(listen, listener)
            }
            checkCurrent()
            check(client.isConnected()) { "Room connection was lost during startup." }
            return true
        } catch (error: Throwable) {
            rollback.forEach { cleanup -> runCatching(cleanup).exceptionOrNull()?.let(error::addSuppressed) }
            throw error
        }
    }

    /** Releases room resources in reverse ownership order, attempting every cleanup after an error. */
    fun close() {
        invalidate()
        listOf<() -> Unit>({ discovery.stop() }, { client.disconnect() }, { files.stop() }, { server.stop() })
            .forEach { runCatching(it) }
    }

    fun start(
        chat: ChatServerConfig,
        file: FileTransferServerConfig,
        join: ChatClientConnectRequest,
        announce: PeerDiscoveryConfig,
        previousDiscovery: PeerDiscoveryConfig?,
        listener: PeerDiscoveryListener,
        token: Long = generation.get(),
    ) {
        check(!server.isRunning() && !client.isConnected() && !files.isRunning()) {
            "Leave the current room before hosting another room."
        }
        val rollback = ArrayDeque<() -> Unit>()
        fun checkCurrent() { if (generation.get() != token) throw CancellationException("Room startup cancelled") }
        try {
            checkCurrent()
            rollback.addFirst { server.stop() }
            server.start(chat)
            checkCurrent()
            rollback.addFirst { files.stop() }
            files.start(file)
            checkCurrent()
            rollback.addFirst { client.disconnect() }
            check(client.connect(join)) { "Could not join the hosted room." }
            checkCurrent()
            rollback.addFirst {
                discovery.stop()
                if (previousDiscovery != null && generation.get() == token) discovery.start(previousDiscovery, listener)
            }
            discovery.start(announce, listener)
            check(discovery.isRunning()) { "Could not start room discovery." }
            check(client.isConnected()) { "Room connection was lost during startup." }
            checkCurrent()
        } catch (error: Throwable) {
            rollback.forEach { cleanup -> runCatching(cleanup).exceptionOrNull()?.let(error::addSuppressed) }
            throw error
        }
    }
}
