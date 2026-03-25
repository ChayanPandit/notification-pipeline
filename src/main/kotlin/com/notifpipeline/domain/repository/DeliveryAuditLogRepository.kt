package com.notifpipeline.domain.repository

import com.notifpipeline.domain.model.DeliveryAuditLog
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DeliveryAuditLogRepository : JpaRepository<DeliveryAuditLog, UUID>