package com.notifpipeline.api

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/webhook-sink")
class WebhookSinkController {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/{recipientId}")
    fun receive(
        @PathVariable recipientId: String,
        @RequestParam(required = false, defaultValue = "200") status: Int,
        @RequestHeader("X-Notif-Delivery-Id", required = false) deliveryId: String?,
        @RequestHeader("X-Notif-Signature", required = false) signature: String?,
        @RequestBody body: String
    ): ResponseEntity<Map<String, Any?>> {
        log.info(
            "[WEBHOOK-SINK] recipient={} status={} deliveryId={} signaturePresent={}",
            recipientId,
            status,
            deliveryId,
            !signature.isNullOrBlank()
        )

        return ResponseEntity.status(status).body(
            mapOf(
                "recipientId" to recipientId,
                "status" to status,
                "deliveryId" to deliveryId,
                "bodyLength" to body.length
            )
        )
    }
}
