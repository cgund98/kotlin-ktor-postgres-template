package com.github.cgund98.template.infrastructure.events.publisher

import com.github.cgund98.template.infrastructure.events.registry.BaseEvent

interface EventPublisher {
    suspend fun <P, T : BaseEvent<P>> publish(event: T)
}
