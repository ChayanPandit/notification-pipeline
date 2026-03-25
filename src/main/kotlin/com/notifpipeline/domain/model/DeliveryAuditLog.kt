package com.notifpipeline.domain.model

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "delivery_audit_log")
class DeliveryAuditLog(

    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "notification_id", nullable = false)
    val notificationId: UUID,

    @Column(name = "channel", nullable = false)
    @Enumerated(EnumType.STRING)
    val channel: DeliveryChannel,

    @Column(name = "event", nullable = false)
    @Enumerated(EnumType.STRING)
    val event: AuditEvent,

    @Column(name = "attempt_number", nullable = false)
    val attemptNumber: Int,

    @Column(columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    val metadata: Map<String, Any> = emptyMap(),

    @Column(name = "occurred_at", nullable = false, updatable = false)
    val occurredAt: Instant = Instant.now()
)

enum class AuditEvent {
    ATTEMPT_STARTED,
    DELIVERED,
    FAILED,
    RETRIED,
    DEAD_LETTERED
}