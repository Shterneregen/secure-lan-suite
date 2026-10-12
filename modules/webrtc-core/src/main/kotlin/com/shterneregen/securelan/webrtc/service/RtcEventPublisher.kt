package com.shterneregen.securelan.webrtc.service

import com.shterneregen.securelan.webrtc.event.RtcEvent

/**
 * Functional interface for dispatching [RtcEvent] notifications to
 * higher-level application components, view models, and state managers.
 */
fun interface RtcEventPublisher {
    /**
     * Publishes a realtime event to registered listeners.
     *
     * @param event the event to dispatch.
     */
    fun publish(event: RtcEvent)
}
