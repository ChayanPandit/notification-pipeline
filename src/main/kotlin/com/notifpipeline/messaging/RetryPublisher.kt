package com.notifpipeline.messaging

import com.notifpipeline.domain.model.DeliveryChannel
import com.notifpipeline.messaging.model.NotificationEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class RetryPublisher(
    private val messagePublisher: MessagePublisher,
    private val retryPolicy: RetryPolicy
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // Returns the topic the event was sent to (for audit logging)
    fun publishForRetryOrDlq(
        channel: DeliveryChannel,
        event: NotificationEvent,
        attemptNumber: Int
    ): BrokerDestination {
        val targetDestination = RetryRouting.retryDestination(channel, attemptNumber)
        val nextAttemptAt = if (isDlq(targetDestination)) null else retryPolicy.nextAttemptAt(attemptNumber, Instant.now())
        val retryEvent = event.copy(nextAttemptAt = nextAttemptAt)

        messagePublisher.publish(targetDestination, retryEvent.recipientId, retryEvent)
        log.info("Published event ${event.notificationId} to $targetDestination for $channel (attempt $attemptNumber)")
        return targetDestination
    }

    fun publishToDlq(channel: DeliveryChannel, event: NotificationEvent): BrokerDestination {
        val targetDestination = RetryRouting.retryDestination(channel, Int.MAX_VALUE)
        messagePublisher.publish(targetDestination, event.recipientId, event.copy(nextAttemptAt = null))
        log.info("Published event ${event.notificationId} to DLQ $targetDestination for $channel")
        return targetDestination
    }

    fun isDlq(destination: BrokerDestination): Boolean =
        destination.name.startsWith("DLQ_")
}
