package com.shterneregen.securelan.desktop.service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

public class DefaultRandomNicknameService implements RandomNicknameService {
    private static final List<String> NICKNAMES = List.of(
            "Alice",
            "Bob",
            "Charlie",
            "Diana",
            "Eve",
            "Frank",
            "Grace",
            "Heidi",
            "Ivan",
            "Judy",
            "Mallory",
            "Niaj",
            "Olivia",
            "Peggy",
            "Quentin",
            "Rupert",
            "Sybil",
            "Trent",
            "Uma",
            "Victor",
            "Walter",
            "Yvonne",
            "Zara",
            "Neo",
            "Trinity",
            "Morpheus",
            "Cipher",
            "Echo",
            "Vector",
            "Nova",
            "Orion",
            "Atlas"
    );

    private final RandomGenerator randomGenerator;

    public DefaultRandomNicknameService() {
        this(new SecureRandom());
    }

    public DefaultRandomNicknameService(RandomGenerator randomGenerator) {
        this.randomGenerator = Objects.requireNonNull(randomGenerator, "randomGenerator must not be null");
    }

    @Override
    public String generate() {
        return NICKNAMES.get(randomGenerator.nextInt(NICKNAMES.size()));
    }
}
