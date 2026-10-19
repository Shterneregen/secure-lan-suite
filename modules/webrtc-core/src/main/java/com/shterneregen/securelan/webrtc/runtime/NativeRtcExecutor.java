package com.shterneregen.securelan.webrtc.runtime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * Serializes JNI operations and resource ownership. Native observers must only post work:
 * JNI can wait for the signaling thread, so that thread must never wait for this worker.
 */
final class NativeRtcExecutor {
    private volatile Thread owner;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
        owner = new Thread(task, "securelan-rtc-native");
        owner.setDaemon(true);
        return owner;
    });

    boolean isOwner() {
        return Thread.currentThread() == owner;
    }

    /** Synchronous entry for application workers; never called from a native observer. */
    void invoke(Runnable action) {
        if (isOwner()) {
            action.run();
            return;
        }
        var completion = new CompletableFuture<Void>();
        executor.execute(() -> {
            try {
                action.run();
                completion.complete(null);
            } catch (Throwable error) {
                completion.completeExceptionally(error);
            }
        });
        completion.join();
    }

    /** Always queues, including reentrant observers called synchronously by JNI. */
    void post(Runnable action) {
        try {
            executor.execute(() -> {
                try { action.run(); }
                catch (Throwable error) { RtcFileLogger.error("Native RTC callback failed", error); }
            });
        } catch (RejectedExecutionException ignored) {
            // The owning engine has already closed every peer and native resource.
        }
    }

    void close(Runnable cleanup) {
        if (executor.isShutdown()) return;
        try { invoke(cleanup); }
        finally { executor.shutdown(); }
    }
}
