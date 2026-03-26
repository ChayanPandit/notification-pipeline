package com.notifpipeline.service

import com.notifpipeline.domain.model.DeliveryChannel
import com.notifpipeline.domain.model.DeliveryStatus
import com.notifpipeline.domain.repository.DeliveryAttemptRepository
import com.notifpipeline.messaging.KafkaTopics
import com.notifpipeline.messaging.model.NotificationEvent
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
class DlqReplayService(
    private val kafkaTemplate: KafkaTemplate<String, NotificationEvent>,
    private val attemptRepository: DeliveryAttemptRepository,
    @Value("\${spring.kafka.bootstrap-servers}") private val bootstrapServers: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun replay(channel: DeliveryChannel): ReplayResult {
        val dlqTopic = dlqTopicFor(channel)
        val deliveryTopic = deliveryTopicFor(channel)

        log.info("Starting DLQ replay for channel $channel from $dlqTopic → $deliveryTopic")

        val consumer = buildConsumer()
        var replayedCount = 0
        var failedCount = 0

        try {
            consumer.subscribe(listOf(dlqTopic))

            // Wait for partition assignment before seeking; otherwise assignment() can be empty and we replay nothing.
            val assignmentDeadlineMs = System.currentTimeMillis() + 5_000
            var assignment = consumer.assignment()
            while (assignment.isEmpty() && System.currentTimeMillis() < assignmentDeadlineMs) {
                consumer.poll(Duration.ofMillis(250))
                assignment = consumer.assignment()
            }
            if (assignment.isEmpty()) {
                log.warn("DLQ replay aborted for $channel: no partition assignment for topic=$dlqTopic after 5s")
                return ReplayResult(
                    channel = channel.name,
                    dlqTopic = dlqTopic,
                    replayedTo = deliveryTopic,
                    replayedCount = 0,
                    failedCount = 0,
                    replayedAt = Instant.now()
                )
            }

            log.info("DLQ replay assigned partitions for $channel: $assignment")

            // Seek to beginning so we replay ALL dead-lettered messages (regardless of committed offsets).
            consumer.seekToBeginning(assignment)
            val beginPositions = assignment.associateWith { tp -> consumer.position(tp) }
            // Snapshot the end offsets at replay start so we do NOT replay any DLQ messages produced during replay.
            val endOffsetsAtStart = consumer.endOffsets(assignment)
            log.info("DLQ replay offsets for $channel: beginPositions=$beginPositions endOffsetsAtStart=$endOffsetsAtStart")

            // Poll with a timeout — stop when no more messages
            val deadline = System.currentTimeMillis() + 10_000 // 10s max
            while (System.currentTimeMillis() < deadline) {
                val records = consumer.poll(Duration.ofMillis(500))
                if (records.isEmpty) {
                    log.info("DLQ replay polled 0 records for $channel; stopping")
                    break
                }

                log.info("DLQ replay polled ${records.count()} records for $channel")

                for (record in records) {
                    val tp = TopicPartition(record.topic(), record.partition())
                    val endOffset = endOffsetsAtStart[tp]
                    // endOffset is the "next offset" after the last record at replay start.
                    // If we see offsets beyond that, they were produced during replay and should be ignored.
                    if (endOffset != null && record.offset() >= endOffset) {
                        log.info(
                            "DLQ replay skipping record produced during replay for $channel: " +
                                "tp=$tp offset=${record.offset()} endOffsetAtStart=$endOffset"
                        )
                        continue
                    }

                    val event = record.value()
                    try {
                        // 1. Re-publish to delivery topic
                        kafkaTemplate.send(deliveryTopic, event.recipientId, event).get()

                        // 2. Reset the latest attempt status so the worker
                        //    treats this as a fresh attempt
                        resetLatestAttempt(event, channel)

                        replayedCount++
                        log.info("Replayed ${event.notificationId} from DLQ to $deliveryTopic")

                    } catch (ex: Exception) {
                        failedCount++
                        log.error("Failed to replay ${event.notificationId}", ex)
                    }
                }

                consumer.commitSync()

                // Stop once we've consumed up to the snapshot end offsets for every assigned partition.
                val done = assignment.all { tp ->
                    val end = endOffsetsAtStart[tp] ?: Long.MAX_VALUE
                    consumer.position(tp) >= end
                }
                if (done) {
                    log.info("DLQ replay reached endOffsetsAtStart for $channel; stopping")
                    break
                }
            }
        } finally {
            consumer.close()
        }

        log.info("DLQ replay complete for $channel — replayed=$replayedCount failed=$failedCount")
        return ReplayResult(
            channel = channel.name,
            dlqTopic = dlqTopic,
            replayedTo = deliveryTopic,
            replayedCount = replayedCount,
            failedCount = failedCount,
            replayedAt = Instant.now()
        )
    }

    private fun resetLatestAttempt(event: NotificationEvent, channel: DeliveryChannel) {
        // Find the latest dead-lettered attempt and mark it FAILED
        // so the worker's attempt counter starts fresh
        val maxAttempt = attemptRepository.findMaxAttemptNumber(event.notificationId, channel)
        if (maxAttempt > 0) {
            attemptRepository
                .findByNotificationIdAndChannelAndAttemptNumber(
                    event.notificationId, channel, maxAttempt
                )
                ?.let { attempt ->
                    attempt.status = DeliveryStatus.FAILED
                    attempt.updatedAt = Instant.now()
                    attemptRepository.save(attempt)
                }
        }
    }

    private fun buildConsumer(): KafkaConsumer<String, NotificationEvent> {
        val deserializer = JsonDeserializer(NotificationEvent::class.java).apply {
            addTrustedPackages("*")
        }
        val props = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "dlq-replay-consumer",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to 100
        )
        return KafkaConsumer(props, StringDeserializer(), deserializer)
    }

    private fun dlqTopicFor(channel: DeliveryChannel) = when (channel) {
        DeliveryChannel.EMAIL   -> KafkaTopics.DLQ_EMAIL
        DeliveryChannel.PUSH    -> KafkaTopics.DLQ_PUSH
        DeliveryChannel.WEBHOOK -> KafkaTopics.DLQ_WEBHOOK
    }

    private fun deliveryTopicFor(channel: DeliveryChannel) = when (channel) {
        DeliveryChannel.EMAIL   -> KafkaTopics.DELIVERY_EMAIL
        DeliveryChannel.PUSH    -> KafkaTopics.DELIVERY_PUSH
        DeliveryChannel.WEBHOOK -> KafkaTopics.DELIVERY_WEBHOOK
    }
}

data class ReplayResult(
    val channel: String,
    val dlqTopic: String,
    val replayedTo: String,
    val replayedCount: Int,
    val failedCount: Int,
    val replayedAt: Instant
)
