package com.notifpipeline.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class IngestRequest(

    @field:NotBlank(message = "eventType must not be blank")
    @field:Size(max = 100)
    val eventType: String,

    @field:NotBlank(message = "recipientId must not be blank")
    @field:Size(max = 255)
    val recipientId: String,

    val payload: Map<String, Any> = emptyMap()
)