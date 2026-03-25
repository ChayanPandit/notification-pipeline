package com.notifpipeline.messaging

import com.notifpipeline.config.ChannelSubscriptionConfig
import com.notifpipeline.messaging.model.NotificationEvent
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class EventRouterConsumer(
    private val kafkaTemplate: KafkaTemplate<String, NotificationEvent>,
    private val subscriptionConfig: ChannelSubscriptionConfig
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [KafkaTopics.INBOUND],
        groupId = "event-router",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun consume(
        record: ConsumerRecord<String, NotificationEvent>,
        ack: Acknowledgment
    ) {
        val event = record.value()
        log.info("Routing event ${event.notificationId} of type ${event.eventType}")

        try {
            val channels = subscriptionConfig.resolveChannels(event.eventType)

            channels.forEach { topic ->
                kafkaTemplate.send(topic, event.recipientId, event)
                log.info("Fanned out ${event.notificationId} to $topic")
            }

            ack.acknowledge()  // only ack after all fan-outs published
        } catch (ex: Exception) {
            log.error("Failed to route event ${event.notificationId}", ex)
            // don't ack — Kafka will redeliver
        }
    }
}