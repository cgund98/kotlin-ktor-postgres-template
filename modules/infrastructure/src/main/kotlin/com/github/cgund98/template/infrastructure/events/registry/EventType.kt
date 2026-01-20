package com.github.cgund98.template.infrastructure.events.registry

import kotlinx.serialization.SerialName

enum class EventType(
    val value: String,
) {
    @SerialName("user.created")
    USER_CREATED("user.created"),

    @SerialName("user.updated")
    USER_UPDATED("user.updated"),

    @SerialName("user.deleted")
    USER_DELETED("user.deleted"),
    ;

    override fun toString(): String = value
}
