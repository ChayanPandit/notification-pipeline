package com.notifpipeline.service

import com.notifpipeline.api.dto.IngestRequest
import com.notifpipeline.domain.model.IdempotencyKey
import com.notifpipeline.domain.model.Notification
import com.notifpipeline.domain.repository.IdempotencyKeyRepository
import com.notifpipeline.domain.repository.NotificationRepository
import com.notifpipeline.messaging.KafkaTopics
import com.notifpipeline.messaging.model.NotificationEvent
import com.notifpipeline.observability.NotificationMetrics
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class IngestService(
    private val notificationRepository: NotificationRepository,
    private val idempotencyKeyRepository: IdempotencyKeyRepository,
    private val kafkaTemplate: KafkaTemplate<String, NotificationEvent>,
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

        // 3. Publish to Kafka
        val event = NotificationEvent(
            notificationId = notification.id,
            idempotencyKey = idempotencyKey,
            eventType = request.eventType,
            recipientId = request.recipientId,
            payload = request.payload
        )

        kafkaTemplate.send(KafkaTopics.INBOUND, notification.recipientId, event)
            .whenComplete { result, ex ->
                if (ex != null) {
                    log.error("Failed to publish event ${notification.id} to Kafka", ex)
                } else {
                    log.info("Published event ${notification.id} to partition ${result.recordMetadata.partition()}")
                }
            }

        log.info("Ingested notification ${notification.id} for recipient ${request.recipientId}")
        metrics.incrementEventsIngested()
        return IngestResult.Accepted(notification.id)
    }
}

sealed class IngestResult {
    data class Accepted(val notificationId: UUID) : IngestResult()
    data class Duplicate(val notificationId: UUID) : IngestResult()
}