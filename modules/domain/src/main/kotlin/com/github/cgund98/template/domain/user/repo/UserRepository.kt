package com.github.cgund98.template.domain.user.repo

import java.util.UUID

data class CreateUserParams(
    val email: String,
    val name: String,
    val age: Int?,
)

data class UpdateUserParams(
    val id: UUID,
    val email: String?,
    val name: String?,
    val age: Int?,
)

interface UserRepository {
    // Basic CRUD
    fun create(params: CreateUserParams): UserEntity

    fun findById(id: UUID): UserEntity?

    fun findByEmail(email: String): UserEntity?

    fun update(params: UpdateUserParams): UserEntity

    fun delete(id: UUID): Boolean

    // Querying
    fun count(filter: UserFilter = UserFilter.Empty): Long

    fun list(
        limit: Int,
        offset: Long,
        filter: UserFilter = UserFilter.Empty,
    ): List<UserEntity>
}

// Data class to handle dynamic filtering without messy params
data class UserFilter(
    val email: String? = null,
) {
    companion object {
        val Empty = UserFilter()
    }
}
