package com.shterneregen.securelan.chat.service;

import java.util.List;

public interface ChatHistoryService {
    void append(String line);
    List<String> getAll();
    String dumpAsText();
    void clear();
}
