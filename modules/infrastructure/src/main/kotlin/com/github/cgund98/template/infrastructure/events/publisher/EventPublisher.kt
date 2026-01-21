package com.github.cgund98.template.infrastructure.events.publisher

import com.github.cgund98.template.infrastructure.events.registry.EventEnvelope

interface EventPublisher {
    suspend fun <T : EventEnvelope<Any>> publish(event: T)
}
