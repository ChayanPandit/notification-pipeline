package com.notifpipeline.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "channel_deliveries")
class ChannelDelivery(

    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "notification_id", nullable = false)
    val notificationId: UUID,

    @Column(name = "channel", nullable = false)
    @Enumerated(EnumType.STRING)
    val channel: DeliveryChannel,

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: ChannelDeliveryStatus = ChannelDeliveryStatus.PENDING,

    @Column(name = "last_attempt_number", nullable = false)
    var lastAttemptNumber: Int = 0,

    @Column(name = "delivered_at")
    var deliveredAt: Instant? = null,

    @Column(name = "last_error")
    var lastError: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)

enum class ChannelDeliveryStatus {
    PENDING,
    PROCESSING,
    DELIVERED,
    FAILED,
    DEAD_LETTERED
}
