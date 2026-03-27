package com.notifpipeline.messaging

import com.notifpipeline.domain.model.DeliveryChannel
import com.notifpipeline.messaging.model.NotificationEvent
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

@Component
class RetryRelayWorker(
    private val kafkaTemplate: KafkaTemplate<String, NotificationEvent>
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [
            KafkaTopics.RETRY_EMAIL_TIER1,
            KafkaTopics.RETRY_EMAIL_TIER2,
            KafkaTopics.RETRY_EMAIL_TIER3
        ],
        groupId = "retry-relay-email",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun relayEmail(record: ConsumerRecord<String, NotificationEvent>, ack: Acknowledgment) {
        relay(DeliveryChannel.EMAIL, record, ack)
    }

    @KafkaListener(
        topics = [
            KafkaTopics.RETRY_PUSH_TIER1,
            KafkaTopics.RETRY_PUSH_TIER2,
            KafkaTopics.RETRY_PUSH_TIER3
        ],
        groupId = "retry-relay-push",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun relayPush(record: ConsumerRecord<String, NotificationEvent>, ack: Acknowledgment) {
        relay(DeliveryChannel.PUSH, record, ack)
    }

    @KafkaListener(
        topics = [
            KafkaTopics.RETRY_WEBHOOK_TIER1,
            KafkaTopics.RETRY_WEBHOOK_TIER2,
            KafkaTopics.RETRY_WEBHOOK_TIER3
        ],
        groupId = "retry-relay-webhook",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun relayWebhook(record: ConsumerRecord<String, NotificationEvent>, ack: Acknowledgment) {
        relay(DeliveryChannel.WEBHOOK, record, ack)
    }

    private fun relay(channel: DeliveryChannel, record: ConsumerRecord<String, NotificationEvent>, ack: Acknowledgment) {
        val event = record.value()
        val dueAt = event.nextAttemptAt

        if (dueAt != null && dueAt.isAfter(Instant.now())) {
            val sleepMs = Duration.between(Instant.now(), dueAt).toMillis().coerceAtMost(5_000)
            if (sleepMs > 0) {
                Thread.sleep(sleepMs)
            }
        }

        if (event.nextAttemptAt != null && event.nextAttemptAt.isAfter(Instant.now())) {
            kafkaTemplate.send(record.topic(), event.recipientId, event).get()
            ack.acknowledge()
            return
        }

        val deliveryEvent = event.copy(nextAttemptAt = null)
        val deliveryTopic = RetryRouting.deliveryTopic(channel)
        kafkaTemplate.send(deliveryTopic, deliveryEvent.recipientId, deliveryEvent).get()
        ack.acknowledge()
        log.info("Released retry event ${event.notificationId} for $channel from ${record.topic()} to $deliveryTopic")
    }
}
