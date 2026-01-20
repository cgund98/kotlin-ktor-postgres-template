package com.github.cgund98.template.domain.user.repo

import kotlinx.datetime.LocalDateTime
import java.util.UUID

data class UserEntity(
    val id: UUID,
    val email: String,
    val name: String,
    val age: Int?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
