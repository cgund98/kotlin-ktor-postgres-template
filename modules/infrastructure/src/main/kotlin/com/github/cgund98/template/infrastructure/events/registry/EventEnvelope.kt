package com.github.cgund98.template.infrastructure.events.registry

import com.github.cgund98.template.core.serialize.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@kotlinx.serialization.Serializable
abstract class EventEnvelope<out P : Any> {
    @Serializable(with = UUIDSerializer::class)
    abstract val id: UUID
    abstract val timestamp: Long
    abstract val type: EventType
    abstract val payload: P

    inline fun <reified P> isPayloadType(): Boolean = payload is P

    companion object {
        /**
         * Generates a new UUID for event identification.
         * Use this in factory functions to ensure consistent ID generation.
         */
        fun generateId(): UUID = UUID.randomUUID()

        /**
         * Generates the current timestamp in milliseconds.
         * Use this in factory functions to ensure consistent timestamp generation.
         */
        fun generateTimestamp(): Long = System.currentTimeMillis()
    }
}
