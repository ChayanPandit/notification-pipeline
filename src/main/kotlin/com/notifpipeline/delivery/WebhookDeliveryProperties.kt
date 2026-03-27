package com.notifpipeline.delivery

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "webhook.delivery")
data class WebhookDeliveryProperties(
    val defaultUrl: String = "http://localhost:8080/api/v1/webhook-sink/default",
    val secret: String = "dev-webhook-secret",
    val connectTimeoutMs: Long = 2_000,
    val readTimeoutMs: Long = 3_000,
    val userAgent: String = "notification-pipeline/webhook"
)
