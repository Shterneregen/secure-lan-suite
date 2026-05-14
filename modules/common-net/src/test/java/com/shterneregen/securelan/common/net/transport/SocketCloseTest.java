package com.shterneregen.securelan.common.net.transport;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocketCloseTest {
    @Test
    void shouldCloseQuietly() {
        AtomicBoolean closed = new AtomicBoolean(false);

        SocketClose.closeQuietly(() -> closed.set(true));

        assertTrue(closed.get());
    }

    @Test
    void shouldSuppressCloseExceptionsAndAcceptNull() {
        assertDoesNotThrow(() -> SocketClose.closeQuietly(null));
        assertDoesNotThrow(() -> SocketClose.closeQuietly(() -> {
            throw new IOException("boom");
        }));
    }
}
