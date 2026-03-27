package com.notifpipeline.service

import com.notifpipeline.api.dto.IngestRequest
import com.notifpipeline.domain.model.IdempotencyKey
import com.notifpipeline.domain.model.Notification
import com.notifpipeline.domain.model.OutboxEvent
import com.notifpipeline.domain.repository.IdempotencyKeyRepository
import com.notifpipeline.domain.repository.NotificationRepository
import com.notifpipeline.domain.repository.OutboxEventRepository
import com.notifpipeline.messaging.BrokerDestination
import com.notifpipeline.observability.NotificationMetrics
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class IngestService(
    private val notificationRepository: NotificationRepository,
    private val idempotencyKeyRepository: IdempotencyKeyRepository,
    private val outboxEventRepository: OutboxEventRepository,
    private val metrics: NotificationMetrics
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun ingest(idempotencyKey: String, request: IngestRequest): IngestResult {

        // 1. Idempotency check
        val existing = idempotencyKeyRepository.findById(idempotencyKey)
        if (existing.isPresent) {
            metrics.incrementDuplicateRejected()
            log.info("Duplicate request detected for idempotency key: $idempotencyKey")
            return IngestResult.Duplicate(existing.get().notificationId)
        }

        // 2. Persist notification + idempotency key atomically
        val notification = notificationRepository.save(
            Notification(
                idempotencyKey = idempotencyKey,
                eventType = request.eventType,
                recipientId = request.recipientId,
                payload = request.payload
            )
        )

        idempotencyKeyRepository.save(
            IdempotencyKey(
                key = idempotencyKey,
                notificationId = notification.id
            )
        )

        outboxEventRepository.save(
            OutboxEvent(
                aggregateType = "notification",
                aggregateId = notification.id,
                topic = BrokerDestination.INBOUND.name,
                messageKey = notification.recipientId,
                eventType = request.eventType,
                payload = mapOf(
                    "notificationId" to notification.id.toString(),
                    "idempotencyKey" to idempotencyKey,
                    "eventType" to request.eventType,
                    "recipientId" to request.recipientId,
                    "payload" to request.payload
                )
            )
        )

        log.info("Ingested notification ${notification.id} for recipient ${request.recipientId} and queued outbox event")
        metrics.incrementEventsIngested()
        return IngestResult.Accepted(notification.id)
    }
}

sealed class IngestResult {
    data class Accepted(val notificationId: UUID) : IngestResult()
    data class Duplicate(val notificationId: UUID) : IngestResult()
}
