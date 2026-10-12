package com.shterneregen.securelan.filetransfer.quickshare

/**
 * Indicates the payload type of a Quick Share item.
 */
enum class QuickShareType {
    /**
     * Shared filesystem file downloadable via HTTP.
     */
    FILE,

    /**
     * Shared text snippet viewable and copyable via HTTP.
     */
    TEXT,
}
