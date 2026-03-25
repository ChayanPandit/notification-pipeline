package com.notifpipeline.config

import com.notifpipeline.messaging.KafkaTopics
import org.springframework.stereotype.Component

@Component
class ChannelSubscriptionConfig {

    // Maps event type → list of delivery topics to fan out to
    private val subscriptions: Map<String, List<String>> = mapOf(
        "ORDER_PLACED"     to listOf(KafkaTopics.DELIVERY_EMAIL, KafkaTopics.DELIVERY_PUSH, KafkaTopics.DELIVERY_WEBHOOK),
        "ORDER_SHIPPED"    to listOf(KafkaTopics.DELIVERY_EMAIL, KafkaTopics.DELIVERY_PUSH),
        "PASSWORD_RESET"   to listOf(KafkaTopics.DELIVERY_EMAIL),
        "PROMO_ALERT"      to listOf(KafkaTopics.DELIVERY_PUSH, KafkaTopics.DELIVERY_WEBHOOK),
    )

    fun resolveChannels(eventType: String): List<String> =
        subscriptions[eventType] ?: listOf(KafkaTopics.DELIVERY_EMAIL) // default fallback
}