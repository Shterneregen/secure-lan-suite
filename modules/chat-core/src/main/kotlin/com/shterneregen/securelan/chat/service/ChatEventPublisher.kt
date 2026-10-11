package com.shterneregen.securelan.chat.service

import com.shterneregen.securelan.chat.event.ChatCoreEvent

/**
 * Functional interface for publishing [ChatCoreEvent]s to the application layer.
 *
 * Implementations are typically lambda expressions or method references provided by the UI or
 * application module when constructing [ChatClientService] or [ChatServerService].
 *
 * Implementations should be thread-safe: events may be published from background I/O threads.
 */
fun interface ChatEventPublisher {

    /**
     * Publishes a [ChatCoreEvent] to the application layer.
     *
     * @param event The event to publish; never `null`.
     */
    fun publish(event: ChatCoreEvent)
}
