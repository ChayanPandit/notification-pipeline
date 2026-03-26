package com.notifpipeline.delivery

import com.notifpipeline.domain.model.DeliveryChannel
import com.notifpipeline.messaging.model.NotificationEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.random.Random

@Service
class WebhookDeliveryService : NotificationChannel {
    private val log = LoggerFactory.getLogger(javaClass)

    override val channel: DeliveryChannel = DeliveryChannel.WEBHOOK

    override fun send(event: NotificationEvent) {
        log.info("[WEBHOOK] POSTing to webhook for recipient ${event.recipientId} for event ${event.notificationId}")
        if (Random.nextFloat() < 0.3f) {
            throw RuntimeException("Webhook endpoint returned 503 (simulated)")
        }
        log.info("[WEBHOOK] Successfully delivered ${event.notificationId}")
    }
}
