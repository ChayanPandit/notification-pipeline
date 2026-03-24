package com.notifpipeline.domain.repository

import com.notifpipeline.domain.model.IdempotencyKey
import org.springframework.data.jpa.repository.JpaRepository

interface IdempotencyKeyRepository : JpaRepository<IdempotencyKey, String>