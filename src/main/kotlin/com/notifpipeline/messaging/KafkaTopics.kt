package com.notifpipeline.messaging

object KafkaTopics {
    const val INBOUND = "notifications.inbound"
    const val DELIVERY_EMAIL = "notifications.delivery.email"
    const val DELIVERY_PUSH = "notifications.delivery.push"
    const val DELIVERY_WEBHOOK = "notifications.delivery.webhook"

    const val RETRY_EMAIL_TIER1 = "notifications.retry.email.tier1"
    const val RETRY_EMAIL_TIER2 = "notifications.retry.email.tier2"
    const val RETRY_EMAIL_TIER3 = "notifications.retry.email.tier3"

    const val RETRY_PUSH_TIER1 = "notifications.retry.push.tier1"
    const val RETRY_PUSH_TIER2 = "notifications.retry.push.tier2"
    const val RETRY_PUSH_TIER3 = "notifications.retry.push.tier3"

    const val RETRY_WEBHOOK_TIER1 = "notifications.retry.webhook.tier1"
    const val RETRY_WEBHOOK_TIER2 = "notifications.retry.webhook.tier2"
    const val RETRY_WEBHOOK_TIER3 = "notifications.retry.webhook.tier3"

    const val DLQ_EMAIL = "notifications.dlq.email"
    const val DLQ_PUSH = "notifications.dlq.push"
    const val DLQ_WEBHOOK = "notifications.dlq.webhook"
}
