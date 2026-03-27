package com.notifpipeline.service

import com.notifpipeline.domain.model.ChannelDelivery
import com.notifpipeline.domain.model.ChannelDeliveryStatus
import com.notifpipeline.domain.model.DeliveryChannel
import com.notifpipeline.domain.repository.ChannelDeliveryRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class ChannelDeliveryIdempotencyService(
    private val channelDeliveryRepository: ChannelDeliveryRepository
) {

    @Transactional
    fun claimForDelivery(notificationId: UUID, channel: DeliveryChannel): DeliveryClaimResult {
        if (!channelDeliveryRepository.existsByNotificationIdAndChannel(notificationId, channel)) {
            try {
                channelDeliveryRepository.save(
                    ChannelDelivery(
                        notificationId = notificationId,
                        channel = channel,
                        status = ChannelDeliveryStatus.PENDING
                    )
                )
            } catch (_: DataIntegrityViolationException) {
                // Another consumer created the row first; lock and evaluate below.
            }
        }

        val delivery = channelDeliveryRepository.findByNotificationIdAndChannel(notificationId, channel)
            ?: error("Missing channel delivery row for notification=$notificationId channel=$channel")

        return when (delivery.status) {
            ChannelDeliveryStatus.DELIVERED -> DeliveryClaimResult.AlreadyDelivered(delivery)
            else -> {
                delivery.status = ChannelDeliveryStatus.PROCESSING
                delivery.updatedAt = Instant.now()
                channelDeliveryRepository.save(delivery)
                DeliveryClaimResult.Claimed(delivery)
            }
        }
    }

    @Transactional
    fun markDelivered(delivery: ChannelDelivery, attemptNumber: Int) {
        delivery.status = ChannelDeliveryStatus.DELIVERED
        delivery.lastAttemptNumber = attemptNumber
        delivery.lastError = null
        delivery.deliveredAt = Instant.now()
        delivery.updatedAt = Instant.now()
        channelDeliveryRepository.save(delivery)
    }

    @Transactional
    fun markFailed(delivery: ChannelDelivery, attemptNumber: Int, errorMessage: String?) {
        delivery.status = ChannelDeliveryStatus.FAILED
        delivery.lastAttemptNumber = attemptNumber
        delivery.lastError = errorMessage
        delivery.updatedAt = Instant.now()
        channelDeliveryRepository.save(delivery)
    }

    @Transactional
    fun markDeadLettered(delivery: ChannelDelivery, attemptNumber: Int, errorMessage: String?) {
        delivery.status = ChannelDeliveryStatus.DEAD_LETTERED
        delivery.lastAttemptNumber = attemptNumber
        delivery.lastError = errorMessage
        delivery.updatedAt = Instant.now()
        channelDeliveryRepository.save(delivery)
    }
}

sealed class DeliveryClaimResult {
    data class Claimed(val delivery: ChannelDelivery) : DeliveryClaimResult()
    data class AlreadyDelivered(val delivery: ChannelDelivery) : DeliveryClaimResult()
}
