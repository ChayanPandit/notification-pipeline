package com.notifpipeline.messaging

import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import kotlin.random.Random

@Component
class RetryPolicy {

    fun nextAttemptAt(attemptNumber: Int, now: Instant = Instant.now()): Instant =
        now.plusMillis(withJitter(delayForAttempt(attemptNumber)).toMillis())

    fun delayForAttempt(attemptNumber: Int): Duration = when (attemptNumber) {
        1 -> Duration.ofSeconds(1)
        2 -> Duration.ofSeconds(10)
        3 -> Duration.ofSeconds(60)
        else -> Duration.ZERO
    }

    private fun withJitter(delay: Duration): Duration {
        if (delay.isZero) {
            return delay
        }
        val millis = delay.toMillis()
        val jitter = (millis * 0.2).toLong()
        val randomized = millis + Random.nextLong(-jitter, jitter + 1)
        return Duration.ofMillis(randomized.coerceAtLeast(0))
    }
}
