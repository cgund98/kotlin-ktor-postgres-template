package com.github.cgund98.template.domain.user

import com.github.cgund98.template.domain.user.repo.UserEntity
import kotlinx.datetime.LocalDateTime
import java.util.UUID

data class User(
    val id: UUID,
    val email: String,
    val name: String,
    val age: Int?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun fromEntity(entity: UserEntity): User =
            User(
                id = entity.id,
                email = entity.email,
                name = entity.name,
                age = entity.age,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
            )
    }
}
