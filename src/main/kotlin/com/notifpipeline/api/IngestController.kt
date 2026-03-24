package com.notifpipeline.api

import com.notifpipeline.api.dto.IngestRequest
import com.notifpipeline.service.IngestResult
import com.notifpipeline.service.IngestService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/events")
class IngestController(
    private val ingestService: IngestService
) {

    @PostMapping
    fun ingest(
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @Valid @RequestBody request: IngestRequest
    ): ResponseEntity<IngestResponse> {

        return when (val result = ingestService.ingest(idempotencyKey, request)) {
            is IngestResult.Accepted -> ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(IngestResponse(notificationId = result.notificationId, status = "ACCEPTED"))

            is IngestResult.Duplicate -> ResponseEntity
                .status(HttpStatus.OK)
                .body(IngestResponse(notificationId = result.notificationId, status = "DUPLICATE"))
        }
    }
}

data class IngestResponse(
    val notificationId: UUID,
    val status: String
)