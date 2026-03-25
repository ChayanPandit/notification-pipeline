package com.notifpipeline.messaging

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
        event: NotificationEvent,
        attemptNumber: Int,
        dlqTopic: String
    ): String {
        val targetTopic = when (attemptNumber) {
            1 -> KafkaTopics.RETRY_TIER1
            2 -> KafkaTopics.RETRY_TIER2
            3 -> KafkaTopics.RETRY_TIER3
            else -> dlqTopic
        }

        kafkaTemplate.send(targetTopic, event.recipientId, event)
        log.info("Published event ${event.notificationId} to $targetTopic (attempt $attemptNumber)")
        return targetTopic
    }

    fun isDlq(topic: String): Boolean =
        topic.startsWith("notifications.dlq")
}