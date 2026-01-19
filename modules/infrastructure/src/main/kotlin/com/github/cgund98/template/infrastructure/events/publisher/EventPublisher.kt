package com.github.cgund98.template.infrastructure.events.publisher

import com.github.cgund98.template.infrastructure.events.registry.EventEnvelope

interface EventPublisher {
    suspend fun <P, T : EventEnvelope<P>> publish(event: T)
}
