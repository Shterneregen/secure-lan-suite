package com.shterneregen.securelan.chat.service.impl;

import com.shterneregen.securelan.chat.service.NicknameRegistryService;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryNicknameRegistryService implements NicknameRegistryService {
    private final Set<String> nicknames = ConcurrentHashMap.newKeySet();

    @Override
    public boolean isAvailable(String nickname) {
        return !nicknames.contains(normalize(nickname));
    }

    @Override
    public boolean register(String nickname) {
        return nicknames.add(normalize(nickname));
    }

    @Override
    public void unregister(String nickname) {
        nicknames.remove(normalize(nickname));
    }

    @Override
    public Set<String> getActiveNicknames() {
        return Set.copyOf(nicknames);
    }

    private String normalize(String nickname) {
        return nickname == null ? "" : nickname.trim().toLowerCase();
    }
}
