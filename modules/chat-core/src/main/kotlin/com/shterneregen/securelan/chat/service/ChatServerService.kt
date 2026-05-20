package com.shterneregen.securelan.chat.service

interface ChatServerService {
    fun start(config: ChatServerConfig)
    fun stop()
    fun isRunning(): Boolean
    fun connectedUsers(): Int
}
