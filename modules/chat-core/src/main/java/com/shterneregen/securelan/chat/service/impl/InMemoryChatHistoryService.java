package com.shterneregen.securelan.chat.service.impl;

import com.shterneregen.securelan.chat.service.ChatHistoryService;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryChatHistoryService implements ChatHistoryService {
    private final CopyOnWriteArrayList<String> lines = new CopyOnWriteArrayList<>();

    @Override
    public void append(String line) {
        lines.add(line);
    }

    @Override
    public List<String> getAll() {
        return List.copyOf(lines);
    }

    @Override
    public String dumpAsText() {
        return String.join(System.lineSeparator(), lines);
    }

    @Override
    public void clear() {
        lines.clear();
    }
}
