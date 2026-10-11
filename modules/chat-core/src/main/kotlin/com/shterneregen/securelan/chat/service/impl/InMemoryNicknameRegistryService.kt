package com.shterneregen.securelan.chat.service.impl

import com.shterneregen.securelan.chat.service.NicknameRegistryService
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe, in-memory implementation of [NicknameRegistryService].
 *
 * Nicknames are normalized (trimmed and lower-cased) before storage so that comparisons are
 * always case-insensitive and whitespace-agnostic.  A `null` or blank nickname normalizes to
 * an empty string, which is treated as permanently unavailable (i.e. [isAvailable] returns
 * `false` and [register] returns `false` for blank or null input).
 *
 * The backing set is a [ConcurrentHashMap]-derived set, making all operations O(1) and safe
 * for concurrent access without external synchronization.
 */
class InMemoryNicknameRegistryService : NicknameRegistryService {

    private val nicknames = ConcurrentHashMap.newKeySet<String>()

    /**
     * Returns `true` if [nickname] is not currently registered.
     *
     * A `null` or blank nickname is never considered available.
     *
     * @param nickname The nickname to check.
     */
    override fun isAvailable(nickname: String?): Boolean = !nicknames.contains(normalize(nickname))

    /**
     * Attempts to atomically register [nickname].
     *
     * @param nickname The nickname to register.
     * @return `true` if the nickname was not already present and has now been registered;
     *         `false` if it was already in use (or was blank/null).
     */
    override fun register(nickname: String?): Boolean = nicknames.add(normalize(nickname))

    /**
     * Releases [nickname], making it available for future registrations.
     *
     * No-op if [nickname] is `null` or was not registered.
     *
     * @param nickname The nickname to unregister.
     */
    override fun unregister(nickname: String?) {
        nicknames.remove(normalize(nickname))
    }

    /**
     * Returns an immutable snapshot of all currently registered nicknames in their normalized
     * (lower-case, trimmed) form.
     *
     * @return A new [Set] of active nicknames.
     */
    override fun getActiveNicknames(): Set<String> = nicknames.toSet()

    /**
     * Normalizes [nickname] by trimming whitespace and converting to lower-case.
     * A `null` input is treated as an empty string.
     */
    private fun normalize(nickname: String?): String = nickname?.trim()?.lowercase() ?: ""
}
