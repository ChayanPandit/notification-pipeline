package com.notifpipeline.delivery

class DeliveryFailureException(
    message: String,
    val retryable: Boolean,
    val metadata: Map<String, Any> = emptyMap(),
    cause: Throwable? = null
) : RuntimeException(message, cause)
