package com.notifpipeline.messaging

import com.notifpipeline.domain.model.DeliveryChannel
import com.notifpipeline.messaging.model.NotificationEvent
import com.notifpipeline.service.ChannelDeliveryService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class PushDeliveryWorker(
    private val channelDeliveryService: ChannelDeliveryService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val channel = DeliveryChannel.PUSH

    @KafkaListener(
        topics = [
            KafkaTopics.DELIVERY_PUSH
        ],
        groupId = "push-delivery-worker",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun consume(record: ConsumerRecord<String, NotificationEvent>, ack: Acknowledgment) {
        val event = record.value()

        try {
            channelDeliveryService.deliver(channel, event)
            ack.acknowledge()
        } catch (ex: Exception) {
            log.error("[PUSH] Delivery orchestration failed for ${event.notificationId}", ex)
        }
    }
}
