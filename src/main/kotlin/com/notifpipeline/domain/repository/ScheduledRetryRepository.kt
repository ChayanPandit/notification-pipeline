package com.notifpipeline.domain.repository

import com.notifpipeline.domain.model.ScheduledRetry
import com.notifpipeline.domain.model.ScheduledRetryStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

interface ScheduledRetryRepository : JpaRepository<ScheduledRetry, UUID> {
    fun findTop100ByStatusInAndDueAtLessThanEqualOrderByDueAtAsc(
        statuses: Collection<ScheduledRetryStatus>,
        dueAt: Instant
    ): List<ScheduledRetry>
}
