package com.notifpipeline.messaging.model

import java.time.Instant
import java.util.UUID

data class NotificationEvent(
    val notificationId: UUID,
    val idempotencyKey: String,
    val eventType: String,
    val recipientId: String,
    val payload: Map<String, Any>,
    val nextAttemptAt: Instant? = null
)
