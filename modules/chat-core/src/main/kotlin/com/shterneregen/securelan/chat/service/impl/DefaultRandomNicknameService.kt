package com.shterneregen.securelan.chat.service.impl

import com.shterneregen.securelan.chat.service.RandomNicknameService
import java.security.SecureRandom
import java.util.Random

class DefaultRandomNicknameService @JvmOverloads constructor(
    // RandomGenerator is only available on Android API 35 and above.
    randomGenerator: Random? = SecureRandom(),
) : RandomNicknameService {
    private val randomGenerator: Random =
        randomGenerator ?: throw NullPointerException("randomGenerator must not be null")

    override fun generate(): String = NICKNAMES[randomGenerator.nextInt(NICKNAMES.size)]

    companion object {
        private val NICKNAMES = listOf(
            "Alice", "Bob", "Charlie", "Diana", "Eve", "Frank", "Grace", "Heidi", "Ivan", "Judy", "Mallory",
            "Niaj", "Olivia", "Peggy", "Quentin", "Rupert", "Sybil", "Trent", "Uma", "Victor", "Walter",
            "Yvonne", "Zara", "Neo", "Trinity", "Morpheus", "Cipher", "Echo", "Vector", "Nova", "Orion", "Atlas",
        )
    }
}
