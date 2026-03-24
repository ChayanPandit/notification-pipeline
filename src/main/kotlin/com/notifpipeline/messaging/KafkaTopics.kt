package com.notifpipeline.messaging

object KafkaTopics {
    const val INBOUND = "notifications.inbound"
    const val DELIVERY_EMAIL = "notifications.delivery.email"
    const val DELIVERY_PUSH = "notifications.delivery.push"
    const val DELIVERY_WEBHOOK = "notifications.delivery.webhook"
    const val RETRY_TIER1 = "notifications.retry.tier1"
    const val RETRY_TIER2 = "notifications.retry.tier2"
    const val RETRY_TIER3 = "notifications.retry.tier3"
    const val DLQ_EMAIL = "notifications.dlq.email"
    const val DLQ_PUSH = "notifications.dlq.push"
    const val DLQ_WEBHOOK = "notifications.dlq.webhook"
}