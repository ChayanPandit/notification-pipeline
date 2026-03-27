package com.notifpipeline.messaging

import com.notifpipeline.domain.model.DeliveryChannel

object RetryRouting {

    fun retryTopic(channel: DeliveryChannel, attemptNumber: Int): String = when (channel) {
        DeliveryChannel.EMAIL -> when (attemptNumber) {
            1 -> KafkaTopics.RETRY_EMAIL_TIER1
            2 -> KafkaTopics.RETRY_EMAIL_TIER2
            3 -> KafkaTopics.RETRY_EMAIL_TIER3
            else -> KafkaTopics.DLQ_EMAIL
        }
        DeliveryChannel.PUSH -> when (attemptNumber) {
            1 -> KafkaTopics.RETRY_PUSH_TIER1
            2 -> KafkaTopics.RETRY_PUSH_TIER2
            3 -> KafkaTopics.RETRY_PUSH_TIER3
            else -> KafkaTopics.DLQ_PUSH
        }
        DeliveryChannel.WEBHOOK -> when (attemptNumber) {
            1 -> KafkaTopics.RETRY_WEBHOOK_TIER1
            2 -> KafkaTopics.RETRY_WEBHOOK_TIER2
            3 -> KafkaTopics.RETRY_WEBHOOK_TIER3
            else -> KafkaTopics.DLQ_WEBHOOK
        }
    }

    fun deliveryTopic(channel: DeliveryChannel): String = when (channel) {
        DeliveryChannel.EMAIL -> KafkaTopics.DELIVERY_EMAIL
        DeliveryChannel.PUSH -> KafkaTopics.DELIVERY_PUSH
        DeliveryChannel.WEBHOOK -> KafkaTopics.DELIVERY_WEBHOOK
    }
}
