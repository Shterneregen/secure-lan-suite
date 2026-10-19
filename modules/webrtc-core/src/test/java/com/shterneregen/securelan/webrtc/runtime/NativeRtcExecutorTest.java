package com.shterneregen.securelan.webrtc.runtime;

import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class NativeRtcExecutorTest {
    @Test
    void nativeCallCanWaitForAnObserverWithoutBlockingTheObserverOnTheWorker() {
        var worker = new NativeRtcExecutor();
        var signaling = Executors.newSingleThreadExecutor();
        var nativeCallReturned = new AtomicBoolean();
        var callbackRan = new CompletableFuture<Boolean>();
        try {
            worker.invoke(() -> {
                // Like setRemoteDescription: JNI waits for an observer on another thread.
                var observer = signaling.submit(() -> worker.post(() -> callbackRan.complete(nativeCallReturned.get())));
                try { observer.get(2, TimeUnit.SECONDS); }
                catch (Exception error) { throw new AssertionError("Native observer blocked", error); }
                nativeCallReturned.set(true);
            });
            assertTrue(callbackRan.get(2, TimeUnit.SECONDS));
        } catch (Exception error) {
            throw new AssertionError(error);
        } finally {
            worker.close(() -> {});
            signaling.shutdownNow();
        }
    }

    @Test
    void synchronousObserverIsDeferredAndCleanupWaitsForTheNativeOperation() throws Exception {
        var worker = new NativeRtcExecutor();
        var nativeCallReturned = new AtomicBoolean();
        var callbackRan = new CompletableFuture<Boolean>();
        var cleanedUp = new AtomicBoolean();
        worker.invoke(() -> {
            worker.post(() -> callbackRan.complete(nativeCallReturned.get()));
            assertFalse(callbackRan.isDone(), "Synchronous JNI observers must not reenter native operations");
            nativeCallReturned.set(true);
        });
        worker.close(() -> {
            assertTrue(nativeCallReturned.get());
            cleanedUp.set(true);
        });
        assertTrue(callbackRan.get(2, TimeUnit.SECONDS));
        assertTrue(cleanedUp.get());
        worker.close(() -> fail("Cleanup ran twice"));
    }
}
