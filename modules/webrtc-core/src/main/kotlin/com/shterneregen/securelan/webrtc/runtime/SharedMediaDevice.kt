package com.shterneregen.securelan.webrtc.runtime

import java.util.concurrent.atomic.AtomicBoolean

/** Owns an explicitly started native device until its last session lease closes. */
class SharedMediaDevice(private val start: Runnable, private val stop: Runnable) : AutoCloseable {
    private var users = 0
    private var closed = false

    @Synchronized fun acquire(): AutoCloseable {
        check(!closed) { "Media device is closed" }
        if (users == 0) start.run()
        users++
        val released = AtomicBoolean()
        return AutoCloseable {
            synchronized(this) {
                if (released.compareAndSet(false, true) && users > 0) {
                    users--
                    if (users == 0) stop.run()
                }
            }
        }
    }

    @Synchronized override fun close() {
        if (closed) return
        closed = true
        if (users > 0) { users = 0; stop.run() }
    }
}
