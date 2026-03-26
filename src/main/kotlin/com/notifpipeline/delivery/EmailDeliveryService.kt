package com.notifpipeline.delivery

import com.notifpipeline.domain.model.DeliveryChannel
import com.notifpipeline.messaging.model.NotificationEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.random.Random

@Service
class EmailDeliveryService : NotificationChannel {
    private val log = LoggerFactory.getLogger(javaClass)

    override val channel: DeliveryChannel = DeliveryChannel.EMAIL

    // Simulates real SMTP — randomly fails 30% of the time so we can
    // actually see retry logic trigger during testing
    override fun send(event: NotificationEvent) {
        log.info("[EMAIL] Sending to recipient ${event.recipientId} for event ${event.notificationId}")
        if (Random.nextFloat() < 0.3f) {
            throw RuntimeException("SMTP connection timeout (simulated)")
        }
        log.info("[EMAIL] Successfully delivered ${event.notificationId}")
    }
}
