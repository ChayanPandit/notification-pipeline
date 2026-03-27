package com.notifpipeline.domain.model

import com.notifpipeline.messaging.model.NotificationEvent
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "scheduled_retries")
class ScheduledRetry(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "notification_id", nullable = false)
    val notificationId: UUID,

    @Column(name = "channel", nullable = false)
    @Enumerated(EnumType.STRING)
    val channel: DeliveryChannel,

    @Column(name = "source_topic", nullable = false)
    val sourceTopic: String,

    @Column(name = "delivery_topic", nullable = false)
    val deliveryTopic: String,

    @Column(name = "message_key", nullable = false)
    val messageKey: String,

    @Column(name = "due_at", nullable = false)
    val dueAt: Instant,

    @Column(columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    val eventPayload: NotificationEvent,

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: ScheduledRetryStatus = ScheduledRetryStatus.PENDING,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Column(name = "published_at")
    var publishedAt: Instant? = null,

    @Column(name = "last_error")
    var lastError: String? = null
)

enum class ScheduledRetryStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
