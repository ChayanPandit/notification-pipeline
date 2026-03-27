package com.notifpipeline.config

import com.notifpipeline.messaging.BrokerDestination
import org.springframework.stereotype.Component

@Component
class ChannelSubscriptionConfig {

    // Maps event type → logical delivery destinations
    private val subscriptions: Map<String, List<BrokerDestination>> = mapOf(
        "ORDER_PLACED"     to listOf(BrokerDestination.DELIVERY_EMAIL, BrokerDestination.DELIVERY_PUSH, BrokerDestination.DELIVERY_WEBHOOK),
        "ORDER_SHIPPED"    to listOf(BrokerDestination.DELIVERY_EMAIL, BrokerDestination.DELIVERY_PUSH),
        "PASSWORD_RESET"   to listOf(BrokerDestination.DELIVERY_EMAIL),
        "PROMO_ALERT"      to listOf(BrokerDestination.DELIVERY_PUSH, BrokerDestination.DELIVERY_WEBHOOK),
    )

    fun resolveChannels(eventType: String): List<BrokerDestination> =
        subscriptions[eventType] ?: listOf(BrokerDestination.DELIVERY_EMAIL)
}
