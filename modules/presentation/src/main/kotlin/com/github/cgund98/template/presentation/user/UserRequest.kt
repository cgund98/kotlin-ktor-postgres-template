package com.github.cgund98.template.presentation.user

import kotlinx.serialization.Serializable

@Serializable
data class UserCreateRequest(
    val email: String,
    val name: String,
    val age: Int?,
)

@Serializable
data class UserUpdateRequest(
    val email: String? = null,
    val name: String? = null,
    val age: Int? = null,
)
