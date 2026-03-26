package com.notifpipeline.domain.repository

import com.notifpipeline.domain.model.OutboxEvent
import com.notifpipeline.domain.model.OutboxStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface OutboxEventRepository : JpaRepository<OutboxEvent, UUID> {
    fun findTop100ByStatusInOrderByCreatedAtAsc(statuses: Collection<OutboxStatus>): List<OutboxEvent>
}
