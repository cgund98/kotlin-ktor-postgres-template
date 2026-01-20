package com.github.cgund98.template.domain.user

import com.github.cgund98.template.domain.user.repo.UserEntity
import com.github.cgund98.template.infrastructure.events.registry.user.UserCreatedPayload
import com.github.cgund98.template.infrastructure.events.registry.user.UserUpdatedPayload
import kotlinx.datetime.LocalDateTime
import java.util.UUID

data class User(
    val id: UUID,
    val email: String,
    val name: String,
    val age: Int?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

// Converters

fun UserEntity.toDomain() =
    User(
        id = id,
        email = email,
        name = name,
        age = age,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun User.toCreatedPayload() =
    UserCreatedPayload(
        id = id,
        email = email,
        name = name,
        age = age,
    )

fun User.toUpdatedPayload() =
    UserUpdatedPayload(
        id = id,
        email = email,
        name = name,
        age = age,
    )
