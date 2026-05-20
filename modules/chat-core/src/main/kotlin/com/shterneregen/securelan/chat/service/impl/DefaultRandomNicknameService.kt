package com.shterneregen.securelan.chat.service.impl

import com.shterneregen.securelan.chat.service.RandomNicknameService
import java.security.SecureRandom
import java.util.Objects
import java.util.random.RandomGenerator

class DefaultRandomNicknameService @JvmOverloads constructor(
    randomGenerator: RandomGenerator = SecureRandom(),
) : RandomNicknameService {
    private val randomGenerator: RandomGenerator = Objects.requireNonNull(randomGenerator, "randomGenerator must not be null")

    override fun generate(): String = NICKNAMES[randomGenerator.nextInt(NICKNAMES.size)]

    companion object {
        private val NICKNAMES = listOf(
            "Alice", "Bob", "Charlie", "Diana", "Eve", "Frank", "Grace", "Heidi", "Ivan", "Judy", "Mallory",
            "Niaj", "Olivia", "Peggy", "Quentin", "Rupert", "Sybil", "Trent", "Uma", "Victor", "Walter",
            "Yvonne", "Zara", "Neo", "Trinity", "Morpheus", "Cipher", "Echo", "Vector", "Nova", "Orion", "Atlas",
        )
    }
}
