package com.notifpipeline.messaging

import com.notifpipeline.domain.model.DeliveryChannel
import com.notifpipeline.messaging.model.NotificationEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class RetryPublisher(
    private val kafkaTemplate: KafkaTemplate<String, NotificationEvent>
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // Returns the topic the event was sent to (for audit logging)
    fun publishForRetryOrDlq(
        channel: DeliveryChannel,
        event: NotificationEvent,
        attemptNumber: Int
    ): String {
        val targetTopic = when (channel) {
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

        kafkaTemplate.send(targetTopic, event.recipientId, event)
        log.info("Published event ${event.notificationId} to $targetTopic for $channel (attempt $attemptNumber)")
        return targetTopic
    }

    fun isDlq(topic: String): Boolean =
        topic.startsWith("notifications.dlq")
}
