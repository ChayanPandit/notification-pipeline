package com.notifpipeline.domain.model

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "notifications")
class Notification(

    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "idempotency_key", nullable = false, unique = true)
    val idempotencyKey: String,

    @Column(name = "event_type", nullable = false)
    val eventType: String,

    @Column(name = "recipient_id", nullable = false)
    val recipientId: String,

    @Column(columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    val payload: Map<String, Any> = emptyMap(),

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: NotificationStatus = NotificationStatus.RECEIVED,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)

enum class NotificationStatus {
    RECEIVED,
    ROUTING_COMPLETE,
    PARTIALLY_DELIVERED,
    FULLY_DELIVERED,
    DEAD_LETTERED
}