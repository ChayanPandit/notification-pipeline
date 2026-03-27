package com.notifpipeline.messaging

import com.notifpipeline.domain.model.DeliveryChannel

object RetryRouting {

    fun retryDestination(channel: DeliveryChannel, attemptNumber: Int): BrokerDestination = when (channel) {
        DeliveryChannel.EMAIL -> when (attemptNumber) {
            1 -> BrokerDestination.RETRY_EMAIL_TIER1
            2 -> BrokerDestination.RETRY_EMAIL_TIER2
            3 -> BrokerDestination.RETRY_EMAIL_TIER3
            else -> BrokerDestination.DLQ_EMAIL
        }
        DeliveryChannel.PUSH -> when (attemptNumber) {
            1 -> BrokerDestination.RETRY_PUSH_TIER1
            2 -> BrokerDestination.RETRY_PUSH_TIER2
            3 -> BrokerDestination.RETRY_PUSH_TIER3
            else -> BrokerDestination.DLQ_PUSH
        }
        DeliveryChannel.WEBHOOK -> when (attemptNumber) {
            1 -> BrokerDestination.RETRY_WEBHOOK_TIER1
            2 -> BrokerDestination.RETRY_WEBHOOK_TIER2
            3 -> BrokerDestination.RETRY_WEBHOOK_TIER3
            else -> BrokerDestination.DLQ_WEBHOOK
        }
    }

    fun deliveryDestination(channel: DeliveryChannel): BrokerDestination = when (channel) {
        DeliveryChannel.EMAIL -> BrokerDestination.DELIVERY_EMAIL
        DeliveryChannel.PUSH -> BrokerDestination.DELIVERY_PUSH
        DeliveryChannel.WEBHOOK -> BrokerDestination.DELIVERY_WEBHOOK
    }
}
