package com.github.cgund98.template.infrastructure.events.registry.user

import com.github.cgund98.template.core.serialize.UUIDSerializer
import com.github.cgund98.template.infrastructure.events.registry.EventEnvelope
import com.github.cgund98.template.infrastructure.events.registry.EventType
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class UserUpdatedPayload(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val email: String,
    val name: String,
    val age: Int?,
)

@Serializable
class UserUpdated(
    @Serializable(with = UUIDSerializer::class)
    override val id: UUID,
    override val timestamp: Long,
    override val payload: UserUpdatedPayload,
) : EventEnvelope<UserUpdatedPayload>() {
    override val type = EventType.USER_UPDATED

    companion object {
        fun create(payload: UserUpdatedPayload): UserUpdated =
            UserUpdated(
                id = EventEnvelope.generateId(),
                timestamp = EventEnvelope.generateTimestamp(),
                payload = payload,
            )
    }
}
