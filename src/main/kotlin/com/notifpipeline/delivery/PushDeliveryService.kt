package com.notifpipeline.delivery

import com.notifpipeline.domain.model.DeliveryChannel
import com.notifpipeline.messaging.model.NotificationEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.random.Random

@Service
class PushDeliveryService : NotificationChannel {
    private val log = LoggerFactory.getLogger(javaClass)

    override val channel: DeliveryChannel = DeliveryChannel.PUSH

    override fun send(event: NotificationEvent) {
        log.info("[PUSH] Sending FCM push to recipient ${event.recipientId} for event ${event.notificationId}")
        if (Random.nextFloat() < 0.3f) {
            throw RuntimeException("FCM service unavailable (simulated)")
        }
        log.info("[PUSH] Successfully delivered ${event.notificationId}")
    }
}
