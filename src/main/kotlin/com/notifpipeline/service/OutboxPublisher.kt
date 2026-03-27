package com.notifpipeline.service

import com.notifpipeline.domain.model.OutboxStatus
import com.notifpipeline.domain.repository.OutboxEventRepository
import com.notifpipeline.messaging.BrokerDestination
import com.notifpipeline.messaging.MessagePublisher
import com.notifpipeline.messaging.model.NotificationEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OutboxPublisher(
    private val outboxEventRepository: OutboxEventRepository,
    private val messagePublisher: MessagePublisher
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${outbox.publisher.fixed-delay-ms:1000}")
    fun publishPendingEvents() {
        val pendingEvents = outboxEventRepository.findTop100ByStatusInOrderByCreatedAtAsc(
            listOf(OutboxStatus.PENDING, OutboxStatus.FAILED)
        )
        pendingEvents.forEach { publish(it.id) }
    }

    @Transactional
    fun publish(outboxEventId: java.util.UUID) {
        val outboxEvent = outboxEventRepository.findById(outboxEventId).orElse(null) ?: return
        if (outboxEvent.status == OutboxStatus.PUBLISHED) {
            return
        }

        @Suppress("UNCHECKED_CAST")
        val notificationPayload = outboxEvent.payload["payload"] as? Map<String, Any> ?: emptyMap()

        val event = NotificationEvent(
            notificationId = outboxEvent.aggregateId,
            idempotencyKey = outboxEvent.payload["idempotencyKey"] as String,
            eventType = outboxEvent.payload["eventType"] as String,
            recipientId = outboxEvent.payload["recipientId"] as String,
            payload = notificationPayload,
            nextAttemptAt = null
        )

        try {
            val destination = BrokerDestination.valueOf(outboxEvent.topic)
            messagePublisher.publishAndWait(destination, outboxEvent.messageKey, event)
            outboxEvent.status = OutboxStatus.PUBLISHED
            outboxEvent.publishedAt = java.time.Instant.now()
            outboxEvent.lastError = null
            outboxEvent.attemptCount += 1
            outboxEvent.updatedAt = java.time.Instant.now()
            outboxEventRepository.save(outboxEvent)
        } catch (ex: Exception) {
            outboxEvent.status = OutboxStatus.FAILED
            outboxEvent.lastError = ex.message
            outboxEvent.attemptCount += 1
            outboxEvent.updatedAt = java.time.Instant.now()
            outboxEventRepository.save(outboxEvent)
            log.error("Failed to publish outbox event ${outboxEvent.id} to destination ${outboxEvent.topic}", ex)
        }
    }
}
