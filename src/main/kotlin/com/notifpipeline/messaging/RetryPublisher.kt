package com.notifpipeline.messaging

import com.notifpipeline.domain.model.DeliveryChannel
import com.notifpipeline.messaging.model.NotificationEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class RetryPublisher(
    private val kafkaTemplate: KafkaTemplate<String, NotificationEvent>,
    private val retryPolicy: RetryPolicy
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // Returns the topic the event was sent to (for audit logging)
    fun publishForRetryOrDlq(
        channel: DeliveryChannel,
        event: NotificationEvent,
        attemptNumber: Int
    ): String {
        val targetTopic = RetryRouting.retryTopic(channel, attemptNumber)
        val nextAttemptAt = if (isDlq(targetTopic)) null else retryPolicy.nextAttemptAt(attemptNumber, Instant.now())
        val retryEvent = event.copy(nextAttemptAt = nextAttemptAt)

        kafkaTemplate.send(targetTopic, retryEvent.recipientId, retryEvent)
        log.info("Published event ${event.notificationId} to $targetTopic for $channel (attempt $attemptNumber)")
        return targetTopic
    }

    fun isDlq(topic: String): Boolean =
        topic.startsWith("notifications.dlq")
}
