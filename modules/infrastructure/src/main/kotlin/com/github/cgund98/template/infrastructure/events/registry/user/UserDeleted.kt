package com.github.cgund98.template.infrastructure.events.registry.user

import com.github.cgund98.template.core.serialize.UUIDSerializer
import com.github.cgund98.template.infrastructure.events.registry.EventEnvelope
import com.github.cgund98.template.infrastructure.events.registry.EventType
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class UserDeletedPayload(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
)

@Serializable
class UserDeleted(
    @Serializable(with = UUIDSerializer::class)
    override val id: UUID,
    override val timestamp: Long,
    override val payload: UserDeletedPayload,
) : EventEnvelope<UserDeletedPayload>() {
    override val type = EventType.USER_DELETED

    companion object {
        fun create(payload: UserDeletedPayload): UserDeleted =
            UserDeleted(
                id = EventEnvelope.generateId(),
                timestamp = EventEnvelope.generateTimestamp(),
                payload = payload,
            )
    }
}
