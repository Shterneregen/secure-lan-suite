package com.shterneregen.securelan.common.net.transport

class SocketClose private constructor() {
    companion object {
        @JvmStatic
        fun closeQuietly(closeable: AutoCloseable?) {
            if (closeable == null) {
                return
            }
            try {
                closeable.close()
            } catch (_: Exception) {
            }
        }
    }
}
