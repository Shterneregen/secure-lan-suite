package com.shterneregen.securelan.chat.service

interface NicknameRegistryService {
    fun isAvailable(nickname: String?): Boolean
    fun register(nickname: String?): Boolean
    fun unregister(nickname: String?)
    fun getActiveNicknames(): Set<String>
}
