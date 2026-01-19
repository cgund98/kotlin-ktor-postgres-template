package com.github.cgund98.template.infrastructure.events.serializer

import com.github.cgund98.template.infrastructure.events.registry.BaseEvent
import kotlin.reflect.KClass

interface EventSerializer {
    /**
     * Converts a string payload into a specific event type.
     */
    fun <P, T : BaseEvent<P>> deserialize(
        raw: String,
        clazz: KClass<T>,
    ): T

    /**
     * Converts an event into a string payload.
     */
    fun <P, T : BaseEvent<P>> serialize(event: T): String
}
