package com.shterneregen.securelan.chat.service.impl

import com.shterneregen.securelan.chat.service.NicknameRegistryService
import java.util.concurrent.ConcurrentHashMap

class InMemoryNicknameRegistryService : NicknameRegistryService {
    private val nicknames = ConcurrentHashMap.newKeySet<String>()

    override fun isAvailable(nickname: String?): Boolean = !nicknames.contains(normalize(nickname))

    override fun register(nickname: String?): Boolean = nicknames.add(normalize(nickname))

    override fun unregister(nickname: String?) {
        nicknames.remove(normalize(nickname))
    }

    override fun getActiveNicknames(): Set<String> = nicknames.toSet()

    private fun normalize(nickname: String?): String = nickname?.trim()?.lowercase() ?: ""
}
