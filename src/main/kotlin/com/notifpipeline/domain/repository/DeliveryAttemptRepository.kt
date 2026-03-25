package com.notifpipeline.domain.repository

import com.notifpipeline.domain.model.DeliveryAttempt
import com.notifpipeline.domain.model.DeliveryChannel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface DeliveryAttemptRepository : JpaRepository<DeliveryAttempt, UUID> {

    @Query("SELECT COALESCE(MAX(a.attemptNumber), 0) FROM DeliveryAttempt a WHERE a.notificationId = :notificationId AND a.channel = :channel")
    fun findMaxAttemptNumber(notificationId: UUID, channel: DeliveryChannel): Int
}