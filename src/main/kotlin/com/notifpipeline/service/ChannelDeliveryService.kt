package com.notifpipeline.service

import com.notifpipeline.delivery.NotificationChannel
import com.notifpipeline.domain.model.AuditEvent
import com.notifpipeline.domain.model.DeliveryAttempt
import com.notifpipeline.domain.model.DeliveryAuditLog
import com.notifpipeline.domain.model.DeliveryChannel
import com.notifpipeline.domain.model.DeliveryStatus
import com.notifpipeline.domain.repository.DeliveryAttemptRepository
import com.notifpipeline.domain.repository.DeliveryAuditLogRepository
import com.notifpipeline.messaging.RetryPublisher
import com.notifpipeline.messaging.model.NotificationEvent
import com.notifpipeline.observability.NotificationMetrics
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ChannelDeliveryService(
    notificationChannels: List<NotificationChannel>,
    private val idempotencyService: ChannelDeliveryIdempotencyService,
    private val attemptRepository: DeliveryAttemptRepository,
    private val auditRepository: DeliveryAuditLogRepository,
    private val retryPublisher: RetryPublisher,
    private val metrics: NotificationMetrics
) {
    private val channelsByType = notificationChannels.associateBy { it.channel }

    fun deliver(channel: DeliveryChannel, event: NotificationEvent) {
        val handler = channelsByType[channel]
            ?: error("No NotificationChannel registered for $channel")
        val claim = idempotencyService.claimForDelivery(event.notificationId, channel)

        if (claim is DeliveryClaimResult.AlreadyDelivered) {
            auditRepository.save(
                DeliveryAuditLog(
                    notificationId = event.notificationId,
                    channel = channel,
                    event = AuditEvent.DELIVERED,
                    attemptNumber = claim.delivery.lastAttemptNumber,
                    metadata = mapOf("idempotentSkip" to true)
                )
            )
            return
        }

        val startTime = System.currentTimeMillis()
        val attemptNumber = attemptRepository.findMaxAttemptNumber(event.notificationId, channel) + 1
        val channelDelivery = (claim as DeliveryClaimResult.Claimed).delivery

        val attempt = attemptRepository.save(
            DeliveryAttempt(
                notificationId = event.notificationId,
                channel = channel,
                attemptNumber = attemptNumber
            )
        )

        auditRepository.save(
            DeliveryAuditLog(
                notificationId = event.notificationId,
                channel = channel,
                event = AuditEvent.ATTEMPT_STARTED,
                attemptNumber = attemptNumber
            )
        )

        try {
            handler.send(event)
            val duration = System.currentTimeMillis() - startTime

            attempt.status = DeliveryStatus.DELIVERED
            attempt.durationMs = duration
            attempt.updatedAt = Instant.now()
            attemptRepository.save(attempt)
            idempotencyService.markDelivered(channelDelivery, attemptNumber)

            auditRepository.save(
                DeliveryAuditLog(
                    notificationId = event.notificationId,
                    channel = channel,
                    event = AuditEvent.DELIVERED,
                    attemptNumber = attemptNumber,
                    metadata = mapOf("durationMs" to duration)
                )
            )

            metrics.incrementDeliverySuccess(channel)
            metrics.recordDeliveryDuration(channel, duration)
        } catch (ex: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val targetTopic = retryPublisher.publishForRetryOrDlq(channel, event, attemptNumber)
            val isDeadLettered = retryPublisher.isDlq(targetTopic)

            attempt.status = if (isDeadLettered) DeliveryStatus.DEAD_LETTERED else DeliveryStatus.FAILED
            attempt.errorMessage = ex.message
            attempt.durationMs = duration
            attempt.updatedAt = Instant.now()
            attemptRepository.save(attempt)
            if (isDeadLettered) {
                idempotencyService.markDeadLettered(channelDelivery, attemptNumber, ex.message)
            } else {
                idempotencyService.markFailed(channelDelivery, attemptNumber, ex.message)
            }

            auditRepository.save(
                DeliveryAuditLog(
                    notificationId = event.notificationId,
                    channel = channel,
                    event = if (isDeadLettered) AuditEvent.DEAD_LETTERED else AuditEvent.RETRIED,
                    attemptNumber = attemptNumber,
                    metadata = mapOf(
                        "error" to (ex.message ?: "unknown"),
                        "nextTopic" to targetTopic
                    )
                )
            )

            metrics.incrementDeliveryFailure(channel)
            metrics.recordDeliveryDuration(channel, duration)
            if (isDeadLettered) {
                metrics.incrementDeadLettered(channel)
            } else {
                metrics.incrementRetry(channel, attemptNumber)
            }
        }
    }
}
