package com.github.cgund98.template.infrastructure.events.consumer

import com.github.cgund98.template.infrastructure.events.registry.EventEnvelope
import kotlin.reflect.KClass

interface EventConsumer {
    suspend fun start()
}

interface EventHandler<EventT : EventEnvelope<Any>> {
    suspend fun handleEvent(event: EventT)
}

data class HandlerBinding<EventT : EventEnvelope<Any>>(
    val handler: EventHandler<EventT>,
    val eventType: KClass<EventT>,
)
