package com.github.cgund98.template.infrastructure.events.consumer

sealed class ConsumerException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class MessageBodyNotFoundException : ConsumerException("Message body not found")

class MessageIdNotFoundException : ConsumerException("Message ID not found")

class DeserializationException(
    message: String,
    cause: Throwable? = null,
) : ConsumerException(message, cause)

class HandlerException(
    message: String,
    cause: Throwable? = null,
) : ConsumerException(message, cause)
