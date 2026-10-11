package com.shterneregen.securelan.chat.service.impl

import com.shterneregen.securelan.chat.service.RandomNicknameService
import java.security.SecureRandom
import java.util.Random

/**
 * Default implementation of [RandomNicknameService] that picks names from a fixed list of 32
 * human-readable nicknames using a [Random] generator.
 *
 * The nickname list consists of classic security-research personas (Alice, Bob, Mallory, …) and
 * pop-culture references.
 *
 * @param randomGenerator The [Random] instance used to select a name.  Defaults to a
 *                        [SecureRandom] instance for unpredictability.  Must not be `null`.
 *
 * @throws NullPointerException if [randomGenerator] is `null`.
 *
 * @constructor Creates a new service with the given random generator.
 */
class DefaultRandomNicknameService @JvmOverloads constructor(
    // RandomGenerator is only available on Android API 35 and above; java.util.Random is used
    // to maintain backward compatibility across all supported platforms.
    randomGenerator: Random? = SecureRandom(),
) : RandomNicknameService {

    private val randomGenerator: Random =
        randomGenerator ?: throw NullPointerException("randomGenerator must not be null")

    /**
     * Generates a random nickname by selecting one entry uniformly from [NICKNAMES].
     *
     * @return A non-null, non-blank nickname string.
     */
    override fun generate(): String = NICKNAMES[randomGenerator.nextInt(NICKNAMES.size)]

    companion object {
        /** The fixed pool of 32 human-readable nicknames. */
        private val NICKNAMES = listOf(
            "Alice", "Bob", "Charlie", "Diana", "Eve", "Frank", "Grace", "Heidi", "Ivan", "Judy", "Mallory",
            "Niaj", "Olivia", "Peggy", "Quentin", "Rupert", "Sybil", "Trent", "Uma", "Victor", "Walter",
            "Yvonne", "Zara", "Neo", "Trinity", "Morpheus", "Cipher", "Echo", "Vector", "Nova", "Orion", "Atlas",
        )
    }
}
