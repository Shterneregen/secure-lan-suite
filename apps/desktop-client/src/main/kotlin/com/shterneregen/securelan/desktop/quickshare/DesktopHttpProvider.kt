package com.shterneregen.securelan.desktop.quickshare

/** Configure JDK process-wide limits before any desktop HTTP server is created. */
internal fun configureDesktopHttpProvider() {
        // JDK 25 initializes these process-wide HTTP bounds when its provider first starts.
        mapOf(
            "jdk.httpserver.maxConnections" to 64,
            "sun.net.httpserver.maxIdleConnections" to 8,
            "sun.net.httpserver.maxReqHeaders" to 32,
            "sun.net.httpserver.maxReqHeaderSize" to 16_384,
            "sun.net.httpserver.maxReqTime" to 10,
            "sun.net.httpserver.maxRspTime" to 60,
        ).forEach { (name, limit) ->
            val configured = System.getProperty(name)?.toIntOrNull()?.takeIf { it > 0 }
            System.setProperty(name, minOf(configured ?: limit, limit).toString())
        }
}
