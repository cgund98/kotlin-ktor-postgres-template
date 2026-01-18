package com.github.cgund98.template.presentation.user

import com.github.cgund98.template.presentation.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class UserCreateRequest(
    val email: String,
    val name: String,
    val age: Int?,
)

@Serializable
data class UserUpdateRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val email: String?,
    val name: String?,
    val age: Int?,
)
