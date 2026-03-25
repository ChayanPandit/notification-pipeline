package com.notifpipeline.observability

import com.notifpipeline.domain.model.DeliveryChannel
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class NotificationMetrics(private val registry: MeterRegistry) {

    // ── Counters ──────────────────────────────────────────────────────────

    fun incrementEventsIngested() =
        Counter.builder("notifications.ingested")
            .description("Total events accepted by the ingest API")
            .register(registry)
            .increment()

    fun incrementDuplicateRejected() =
        Counter.builder("notifications.duplicate")
            .description("Total duplicate events rejected by idempotency check")
            .register(registry)
            .increment()

    fun incrementDeliverySuccess(channel: DeliveryChannel) =
        Counter.builder("notifications.delivery.success")
            .tag("channel", channel.name.lowercase())
            .description("Successful deliveries per channel")
            .register(registry)
            .increment()

    fun incrementDeliveryFailure(channel: DeliveryChannel) =
        Counter.builder("notifications.delivery.failure")
            .tag("channel", channel.name.lowercase())
            .description("Failed delivery attempts per channel")
            .register(registry)
            .increment()

    fun incrementRetry(channel: DeliveryChannel, tier: Int) =
        Counter.builder("notifications.retry")
            .tag("channel", channel.name.lowercase())
            .tag("tier", tier.toString())
            .description("Retry attempts per channel and tier")
            .register(registry)
            .increment()

    fun incrementDeadLettered(channel: DeliveryChannel) =
        Counter.builder("notifications.dlq")
            .tag("channel", channel.name.lowercase())
            .description("Messages sent to DLQ per channel")
            .register(registry)
            .increment()

    // ── Timers ────────────────────────────────────────────────────────────

    fun recordDeliveryDuration(channel: DeliveryChannel, durationMs: Long) =
        Timer.builder("notifications.delivery.duration")
            .tag("channel", channel.name.lowercase())
            .description("Delivery attempt duration in ms")
            .register(registry)
            .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS)
}