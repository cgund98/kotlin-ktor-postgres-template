package com.github.cgund98.template.infrastructure.events.registry.user

import com.github.cgund98.template.core.serialize.UUIDSerializer
import com.github.cgund98.template.infrastructure.events.registry.BaseEvent
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
    override val payload: UserUpdatedPayload,
) : BaseEvent<UserUpdatedPayload>() {
    override val type = EventType.USER_UPDATED
}
