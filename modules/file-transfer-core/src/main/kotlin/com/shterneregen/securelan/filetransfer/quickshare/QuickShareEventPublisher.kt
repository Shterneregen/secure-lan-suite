package com.shterneregen.securelan.filetransfer.quickshare

/**
 * Functional listener interface for receiving [QuickShareEvent] notifications.
 */
fun interface QuickShareEventPublisher {
    /**
     * Publishes a Quick Share event.
     *
     * @param event The [QuickShareEvent] to publish.
     */
    fun publish(event: QuickShareEvent)

    companion object {
        /**
         * Returns a no-op publisher that ignores all events.
         */
        @JvmStatic
        fun noOp(): QuickShareEventPublisher = QuickShareEventPublisher { }
    }
}
