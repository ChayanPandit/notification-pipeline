package com.notifpipeline.delivery

import com.notifpipeline.domain.model.DeliveryChannel
import com.notifpipeline.messaging.model.NotificationEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.util.HexFormat
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class WebhookDeliveryService(
    private val properties: WebhookDeliveryProperties,
    private val objectMapper: ObjectMapper
) : NotificationChannel {
    private val log = LoggerFactory.getLogger(javaClass)
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(properties.connectTimeoutMs))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build()

    override val channel: DeliveryChannel = DeliveryChannel.WEBHOOK

    override fun send(event: NotificationEvent) {
        val targetUrl = resolveTargetUrl(event)
        val timestamp = Instant.now().toString()
        val body = objectMapper.writeValueAsString(
            mapOf(
                "notificationId" to event.notificationId,
                "eventType" to event.eventType,
                "recipientId" to event.recipientId,
                "payload" to event.payload
            )
        )
        val signature = sign(timestamp, body)

        val request = HttpRequest.newBuilder()
            .uri(URI.create(targetUrl))
            .timeout(Duration.ofMillis(properties.readTimeoutMs))
            .header("Content-Type", "application/json")
            .header("User-Agent", properties.userAgent)
            .header("Idempotency-Key", event.idempotencyKey)
            .header("X-Notif-Delivery-Id", event.notificationId.toString())
            .header("X-Notif-Event-Type", event.eventType)
            .header("X-Notif-Timestamp", timestamp)
            .header("X-Notif-Signature", signature)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        log.info("[WEBHOOK] POST {} for event {}", targetUrl, event.notificationId)

        val response = try {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (ex: Exception) {
            throw DeliveryFailureException(
                message = "Webhook transport failure: ${ex.message}",
                retryable = true,
                metadata = mapOf("url" to targetUrl),
                cause = ex
            )
        }

        when {
            response.statusCode() in 200..299 -> {
                log.info("[WEBHOOK] Delivered {} status={}", event.notificationId, response.statusCode())
            }
            response.statusCode() in listOf(301, 302, 307, 308) -> {
                throw DeliveryFailureException(
                    message = "Webhook redirect not followed: status=${response.statusCode()}",
                    retryable = false,
                    metadata = mapOf(
                        "url" to targetUrl,
                        "statusCode" to response.statusCode(),
                        "location" to (response.headers().firstValue("location").orElse(""))
                    )
                )
            }
            response.statusCode() == 408 || response.statusCode() == 425 || response.statusCode() == 429 ||
                response.statusCode() in 500..599 -> {
                throw DeliveryFailureException(
                    message = "Webhook retryable response: status=${response.statusCode()}",
                    retryable = true,
                    metadata = mapOf("url" to targetUrl, "statusCode" to response.statusCode())
                )
            }
            else -> {
                throw DeliveryFailureException(
                    message = "Webhook non-retryable response: status=${response.statusCode()}",
                    retryable = false,
                    metadata = mapOf("url" to targetUrl, "statusCode" to response.statusCode())
                )
            }
        }
    }

    private fun resolveTargetUrl(event: NotificationEvent): String =
        event.payload["webhookUrl"] as? String ?: "${properties.defaultUrl.removeSuffix("/")}/${event.recipientId}"

    private fun sign(timestamp: String, body: String): String {
        val payload = "$timestamp.$body".toByteArray(StandardCharsets.UTF_8)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(properties.secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return HexFormat.of().formatHex(mac.doFinal(payload))
    }
}
