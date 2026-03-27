package com.notifpipeline.service

import com.notifpipeline.domain.model.DeliveryChannel
import com.notifpipeline.domain.model.ScheduledRetry
import com.notifpipeline.domain.model.ScheduledRetryStatus
import com.notifpipeline.domain.repository.ScheduledRetryRepository
import com.notifpipeline.messaging.model.NotificationEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class ScheduledRetryService(
    private val scheduledRetryRepository: ScheduledRetryRepository,
    private val kafkaTemplate: KafkaTemplate<String, NotificationEvent>
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun schedule(
        channel: DeliveryChannel,
        sourceTopic: String,
        deliveryTopic: String,
        messageKey: String,
        event: NotificationEvent
    ) {
        val dueAt = event.nextAttemptAt ?: Instant.now()
        scheduledRetryRepository.save(
            ScheduledRetry(
                notificationId = event.notificationId,
                channel = channel,
                sourceTopic = sourceTopic,
                deliveryTopic = deliveryTopic,
                messageKey = messageKey,
                dueAt = dueAt,
                eventPayload = event
            )
        )
    }

    @Scheduled(fixedDelayString = "\${retry.release.fixed-delay-ms:1000}")
    fun releaseDueRetries() {
        val dueRetries = scheduledRetryRepository.findTop100ByStatusInAndDueAtLessThanEqualOrderByDueAtAsc(
            listOf(ScheduledRetryStatus.PENDING, ScheduledRetryStatus.FAILED),
            Instant.now()
        )
        dueRetries.forEach { release(it.id) }
    }

    @Transactional
    fun release(id: java.util.UUID) {
        val retry = scheduledRetryRepository.findById(id).orElse(null) ?: return
        if (retry.status == ScheduledRetryStatus.PUBLISHED) {
            return
        }

        try {
            kafkaTemplate.send(
                retry.deliveryTopic,
                retry.messageKey,
                retry.eventPayload.copy(nextAttemptAt = null)
            ).get()
            retry.status = ScheduledRetryStatus.PUBLISHED
            retry.publishedAt = Instant.now()
            retry.lastError = null
            retry.updatedAt = Instant.now()
            scheduledRetryRepository.save(retry)
            log.info("Released scheduled retry {} for {} to {}", retry.notificationId, retry.channel, retry.deliveryTopic)
        } catch (ex: Exception) {
            retry.status = ScheduledRetryStatus.FAILED
            retry.lastError = ex.message
            retry.updatedAt = Instant.now()
            scheduledRetryRepository.save(retry)
            log.error("Failed to release scheduled retry ${retry.id}", ex)
        }
    }
}
