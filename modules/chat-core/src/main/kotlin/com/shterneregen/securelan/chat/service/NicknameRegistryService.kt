package com.shterneregen.securelan.chat.service

/**
 * Service that tracks which nicknames are currently in use in a chat room.
 *
 * Nickname comparison is case-insensitive and whitespace is trimmed before any operation.
 * All methods must be thread-safe.
 */
interface NicknameRegistryService {

    /**
     * Returns `true` if [nickname] is not currently registered.
     *
     * A `null` or blank nickname is never considered available.
     *
     * @param nickname The nickname to check.
     */
    fun isAvailable(nickname: String?): Boolean

    /**
     * Attempts to register [nickname] as taken.
     *
     * @param nickname The nickname to register.
     * @return `true` if the nickname was successfully registered (was available);
     *         `false` if it was already in use.
     */
    fun register(nickname: String?): Boolean

    /**
     * Releases a previously registered [nickname], making it available again.
     *
     * If [nickname] is `null` or was not registered, this call is a no-op.
     *
     * @param nickname The nickname to unregister.
     */
    fun unregister(nickname: String?)

    /**
     * Returns an immutable snapshot of all currently registered (active) nicknames.
     *
     * @return A new set of active nickname strings in their normalized (lower-case, trimmed) form.
     */
    fun getActiveNicknames(): Set<String>
}
