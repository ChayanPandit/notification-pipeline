package com.notifpipeline.domain.model

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "idempotency_keys")
class IdempotencyKey(

    @Id
    @Column(name = "key")
    val key: String,

    @Column(name = "notification_id", nullable = false)
    val notificationId: UUID,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant = Instant.now().plusSeconds(86400) // 24h TTL
)