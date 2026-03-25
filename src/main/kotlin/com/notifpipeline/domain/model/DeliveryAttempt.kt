package com.notifpipeline.domain.model

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "delivery_attempts")
class DeliveryAttempt(

    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "notification_id", nullable = false)
    val notificationId: UUID,

    @Column(name = "channel", nullable = false)
    @Enumerated(EnumType.STRING)
    val channel: DeliveryChannel,

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: DeliveryStatus = DeliveryStatus.IN_PROGRESS,

    @Column(name = "attempt_number", nullable = false)
    val attemptNumber: Int = 1,

    @Column(name = "next_retry_at")
    var nextRetryAt: Instant? = null,

    @Column(name = "error_message")
    var errorMessage: String? = null,

    @Column(name = "duration_ms")
    var durationMs: Long? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)

enum class DeliveryChannel { EMAIL, PUSH, WEBHOOK }

enum class DeliveryStatus { IN_PROGRESS, DELIVERED, FAILED, DEAD_LETTERED }