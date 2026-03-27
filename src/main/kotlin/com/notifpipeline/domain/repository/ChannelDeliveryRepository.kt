package com.notifpipeline.domain.repository

import com.notifpipeline.domain.model.ChannelDelivery
import com.notifpipeline.domain.model.DeliveryChannel
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import java.util.UUID

interface ChannelDeliveryRepository : JpaRepository<ChannelDelivery, UUID> {

    fun existsByNotificationIdAndChannel(notificationId: UUID, channel: DeliveryChannel): Boolean

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByNotificationIdAndChannel(notificationId: UUID, channel: DeliveryChannel): ChannelDelivery?
}
