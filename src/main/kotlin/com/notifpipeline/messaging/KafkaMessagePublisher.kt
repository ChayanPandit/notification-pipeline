package com.notifpipeline.messaging

import com.notifpipeline.messaging.model.NotificationEvent
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class KafkaMessagePublisher(
    private val kafkaTemplate: KafkaTemplate<String, NotificationEvent>,
    private val destinationResolver: BrokerDestinationResolver
) : MessagePublisher {
    override fun publish(destination: BrokerDestination, key: String, event: NotificationEvent) {
        kafkaTemplate.send(destinationResolver.resolve(destination), key, event)
    }

    override fun publishAndWait(destination: BrokerDestination, key: String, event: NotificationEvent) {
        kafkaTemplate.send(destinationResolver.resolve(destination), key, event).get()
    }
}
