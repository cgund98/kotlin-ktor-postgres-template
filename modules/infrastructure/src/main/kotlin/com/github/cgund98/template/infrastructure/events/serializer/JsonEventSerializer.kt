package com.github.cgund98.template.infrastructure.events.serializer

import com.github.cgund98.template.infrastructure.events.registry.BaseEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

class JsonEventSerializer(
    private val json: Json,
) : EventSerializer {
    override fun <P, T : BaseEvent<P>> deserialize(
        raw: String,
        clazz: KClass<T>,
    ): T {
        val serializer = json.serializersModule.serializer(clazz.java)
        @Suppress("UNCHECKED_CAST")
        return json.decodeFromString(serializer, raw) as T
    }

    override fun <P, T : BaseEvent<P>> serialize(event: T): String {
        val serializer = json.serializersModule.serializer(event::class.java)
        return json.encodeToString(serializer, event)
    }
}
