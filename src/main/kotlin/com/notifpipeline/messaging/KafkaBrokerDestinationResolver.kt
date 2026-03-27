package com.notifpipeline.messaging

import org.springframework.stereotype.Component

@Component
class KafkaBrokerDestinationResolver : BrokerDestinationResolver {
    override fun resolve(destination: BrokerDestination): String = when (destination) {
        BrokerDestination.INBOUND -> KafkaTopics.INBOUND
        BrokerDestination.DELIVERY_EMAIL -> KafkaTopics.DELIVERY_EMAIL
        BrokerDestination.DELIVERY_PUSH -> KafkaTopics.DELIVERY_PUSH
        BrokerDestination.DELIVERY_WEBHOOK -> KafkaTopics.DELIVERY_WEBHOOK
        BrokerDestination.RETRY_EMAIL_TIER1 -> KafkaTopics.RETRY_EMAIL_TIER1
        BrokerDestination.RETRY_EMAIL_TIER2 -> KafkaTopics.RETRY_EMAIL_TIER2
        BrokerDestination.RETRY_EMAIL_TIER3 -> KafkaTopics.RETRY_EMAIL_TIER3
        BrokerDestination.RETRY_PUSH_TIER1 -> KafkaTopics.RETRY_PUSH_TIER1
        BrokerDestination.RETRY_PUSH_TIER2 -> KafkaTopics.RETRY_PUSH_TIER2
        BrokerDestination.RETRY_PUSH_TIER3 -> KafkaTopics.RETRY_PUSH_TIER3
        BrokerDestination.RETRY_WEBHOOK_TIER1 -> KafkaTopics.RETRY_WEBHOOK_TIER1
        BrokerDestination.RETRY_WEBHOOK_TIER2 -> KafkaTopics.RETRY_WEBHOOK_TIER2
        BrokerDestination.RETRY_WEBHOOK_TIER3 -> KafkaTopics.RETRY_WEBHOOK_TIER3
        BrokerDestination.DLQ_EMAIL -> KafkaTopics.DLQ_EMAIL
        BrokerDestination.DLQ_PUSH -> KafkaTopics.DLQ_PUSH
        BrokerDestination.DLQ_WEBHOOK -> KafkaTopics.DLQ_WEBHOOK
    }
}
