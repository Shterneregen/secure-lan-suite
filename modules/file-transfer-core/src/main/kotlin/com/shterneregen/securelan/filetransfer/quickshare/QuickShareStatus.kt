package com.shterneregen.securelan.filetransfer.quickshare

/**
 * Lifecycle status of an active or terminated Quick Share item.
 */
enum class QuickShareStatus {
    /**
     * Share is currently active and reachable for viewing or download.
     */
    ACTIVE,

    /**
     * Share was explicitly stopped by the local user.
     */
    STOPPED,

    /**
     * Share expired due to its configured time-to-live expiring.
     */
    EXPIRED,

    /**
     * Share was terminated because the configured maximum access count was reached.
     */
    LIMIT_REACHED,
}
