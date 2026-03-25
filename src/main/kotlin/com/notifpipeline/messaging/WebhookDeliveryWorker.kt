package com.notifpipeline.messaging

import com.notifpipeline.delivery.WebhookDeliveryService
import com.notifpipeline.domain.model.*
import com.notifpipeline.domain.repository.DeliveryAttemptRepository
import com.notifpipeline.domain.repository.DeliveryAuditLogRepository
import com.notifpipeline.messaging.model.NotificationEvent
import com.notifpipeline.observability.NotificationMetrics
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class WebhookDeliveryWorker(
    private val webhookService: WebhookDeliveryService,
    private val attemptRepository: DeliveryAttemptRepository,
    private val auditRepository: DeliveryAuditLogRepository,
    private val retryPublisher: RetryPublisher,
    private val metrics: NotificationMetrics
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val channel = DeliveryChannel.WEBHOOK
    private val dlqTopic = KafkaTopics.DLQ_WEBHOOK

    @KafkaListener(
        topics = [
            KafkaTopics.DELIVERY_WEBHOOK,
            KafkaTopics.RETRY_TIER1,
            KafkaTopics.RETRY_TIER2,
            KafkaTopics.RETRY_TIER3
        ],
        groupId = "webhook-delivery-worker",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun consume(record: ConsumerRecord<String, NotificationEvent>, ack: Acknowledgment) {
        val event = record.value()
        val startTime = System.currentTimeMillis()
        val attemptNumber = attemptRepository
            .findMaxAttemptNumber(event.notificationId, channel) + 1

        log.info("[WEBHOOK] Attempt #$attemptNumber for event ${event.notificationId}")

        val attempt = attemptRepository.save(DeliveryAttempt(
            notificationId = event.notificationId,
            channel = channel,
            attemptNumber = attemptNumber
        ))

        auditRepository.save(DeliveryAuditLog(
            notificationId = event.notificationId,
            channel = channel,
            event = AuditEvent.ATTEMPT_STARTED,
            attemptNumber = attemptNumber
        ))

        try {
            webhookService.send(event)
            val duration = System.currentTimeMillis() - startTime

            attempt.status = DeliveryStatus.DELIVERED
            attempt.durationMs = duration
            attempt.updatedAt = Instant.now()
            attemptRepository.save(attempt)

            auditRepository.save(DeliveryAuditLog(
                notificationId = event.notificationId,
                channel = channel,
                event = AuditEvent.DELIVERED,
                attemptNumber = attemptNumber,
                metadata = mapOf("durationMs" to duration)
            ))

            ack.acknowledge()
            metrics.incrementDeliverySuccess(channel)
            metrics.recordDeliveryDuration(channel, duration)

            log.info("[WEBHOOK] Delivered ${event.notificationId} in ${duration}ms")

        } catch (ex: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val targetTopic = retryPublisher.publishForRetryOrDlq(event, attemptNumber, dlqTopic)
            val isDeadLettered = retryPublisher.isDlq(targetTopic)

            attempt.status = if (isDeadLettered) DeliveryStatus.DEAD_LETTERED else DeliveryStatus.FAILED
            attempt.errorMessage = ex.message
            attempt.durationMs = duration
            attempt.updatedAt = Instant.now()
            attemptRepository.save(attempt)

            auditRepository.save(DeliveryAuditLog(
                notificationId = event.notificationId,
                channel = channel,
                event = if (isDeadLettered) AuditEvent.DEAD_LETTERED else AuditEvent.RETRIED,
                attemptNumber = attemptNumber,
                metadata = mapOf("error" to (ex.message ?: "unknown"), "nextTopic" to targetTopic)
            ))

            ack.acknowledge()
            metrics.incrementDeliveryFailure(channel)
            metrics.recordDeliveryDuration(channel, duration)
            if (isDeadLettered) {
                metrics.incrementDeadLettered(channel)
            } else {
                metrics.incrementRetry(channel, attemptNumber)
            }

            log.warn("[WEBHOOK] Failed attempt #$attemptNumber for ${event.notificationId}, sent to $targetTopic")
        }
    }
}