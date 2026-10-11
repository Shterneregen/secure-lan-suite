package com.shterneregen.securelan.chat.service

/**
 * Service that generates random human-readable nicknames.
 *
 * Typical usage is to pre-fill the nickname field in the connection dialog when no persistent
 * nickname has been configured.
 */
interface RandomNicknameService {

    /**
     * Generates and returns a random nickname.
     *
     * @return A non-null, non-blank nickname string chosen from the implementation's list.
     */
    fun generate(): String
}
