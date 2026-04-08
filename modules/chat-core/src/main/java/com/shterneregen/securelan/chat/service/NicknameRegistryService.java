package com.shterneregen.securelan.chat.service;

import java.util.Set;

public interface NicknameRegistryService {
    boolean isAvailable(String nickname);
    boolean register(String nickname);
    void unregister(String nickname);
    Set<String> getActiveNicknames();
}
