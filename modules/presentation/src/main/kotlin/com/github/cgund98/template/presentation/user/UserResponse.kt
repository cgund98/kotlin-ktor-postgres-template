package com.github.cgund98.template.presentation.user

import com.github.cgund98.template.domain.user.User
import com.github.cgund98.template.presentation.PaginationMetadata
import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val name: String,
    val age: Int?,
    val createdAt: String,
    val updatedAt: String,
)

fun User.toResponse() =
    UserResponse(
        id = id.toString(),
        email = email,
        name = name,
        age = age,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
    )

@Serializable
data class GetUserResponse(
    val user: UserResponse?,
)

@Serializable
data class ListUsersResponse(
    val users: List<UserResponse>,
    val pagination: PaginationMetadata,
)
