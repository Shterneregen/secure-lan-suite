package com.shterneregen.securelan.common.net.transport

/**
 * Utility functions for safely and quietly closing resources without throwing checked or runtime exceptions.
 */
class SocketClose private constructor() {
    companion object {
        /**
         * Closes the given [closeable] quietly, suppressing any exceptions.
         *
         * If [closeable] is `null`, this operation is a no-op.
         *
         * @param closeable the resource to close, or `null`.
         */
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

/**
 * Extension function to quietly close an [AutoCloseable] suppressing any exceptions.
 */
fun AutoCloseable?.closeQuietly() {
    SocketClose.closeQuietly(this)
}
