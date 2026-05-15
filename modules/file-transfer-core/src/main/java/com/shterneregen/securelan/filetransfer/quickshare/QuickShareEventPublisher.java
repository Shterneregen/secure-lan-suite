package com.shterneregen.securelan.filetransfer.quickshare;

@FunctionalInterface
public interface QuickShareEventPublisher {
    void publish(QuickShareEvent event);

    static QuickShareEventPublisher noOp() {
        return event -> {
        };
    }
}
