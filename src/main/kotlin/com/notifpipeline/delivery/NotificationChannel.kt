package com.notifpipeline.delivery

import com.notifpipeline.domain.model.DeliveryChannel
import com.notifpipeline.messaging.model.NotificationEvent

interface NotificationChannel {
    val channel: DeliveryChannel

    fun send(event: NotificationEvent)
}
