package com.notifpipeline.messaging

import com.notifpipeline.messaging.model.NotificationEvent

interface MessagePublisher {
    fun publish(destination: BrokerDestination, key: String, event: NotificationEvent)
    fun publishAndWait(destination: BrokerDestination, key: String, event: NotificationEvent)
}
