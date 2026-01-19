package com.github.cgund98.template.infrastructure.events.registry

import com.github.cgund98.template.core.serialize.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@kotlinx.serialization.Serializable
abstract class BaseEvent<T> {
    @Serializable(with = UUIDSerializer::class)
    val id: UUID = UUID.randomUUID()
    val timestamp: Long = System.currentTimeMillis()

    abstract val type: EventType
    abstract val payload: T

    inline fun <reified T> isPayloadType(): Boolean = payload is T
}
