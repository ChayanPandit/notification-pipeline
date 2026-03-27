package com.notifpipeline.messaging

import com.notifpipeline.domain.model.DeliveryChannel
import com.notifpipeline.messaging.model.NotificationEvent
import com.notifpipeline.service.ScheduledRetryService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class RetryRelayWorker(
    private val scheduledRetryService: ScheduledRetryService
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
        val deliveryTopic = RetryRouting.deliveryTopic(channel)
        scheduledRetryService.schedule(
            channel = channel,
            sourceTopic = record.topic(),
            deliveryTopic = deliveryTopic,
            messageKey = event.recipientId,
            event = event
        )
        ack.acknowledge()
        log.info("Scheduled retry event {} for {} from {} dueAt={}", event.notificationId, channel, record.topic(), event.nextAttemptAt)
    }
}
