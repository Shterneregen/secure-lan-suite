package com.shterneregen.securelan.common.model

/** Delivery state of a chat message. */
enum class DeliveryStatus {
    /** The message exists locally but has not been sent. */
    CREATED,
    /** The message has been sent. */
    SENT,
    /** Delivery to the recipient has been confirmed. */
    DELIVERED,
    /** The recipient has read the message. */
    READ,
    /** Message delivery failed. */
    FAILED,
}
