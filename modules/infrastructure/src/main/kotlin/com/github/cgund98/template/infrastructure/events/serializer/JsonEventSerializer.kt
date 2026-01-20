package com.github.cgund98.template.infrastructure.events.serializer

import com.github.cgund98.template.infrastructure.events.registry.EventEnvelope
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

class JsonEventSerializer(
    private val json: Json,
) : EventSerializer {
    override fun <P, T : EventEnvelope<P>> deserialize(
        raw: String,
        clazz: KClass<T>,
    ): T {
        val serializer = json.serializersModule.serializer(clazz.java)
        @Suppress("UNCHECKED_CAST")
        return json.decodeFromString(serializer, raw) as T
    }

    override fun <P, T : EventEnvelope<P>> serialize(event: T): String {
        val serializer = json.serializersModule.serializer(event::class.java)
        return json.encodeToString(serializer, event)
    }
}
