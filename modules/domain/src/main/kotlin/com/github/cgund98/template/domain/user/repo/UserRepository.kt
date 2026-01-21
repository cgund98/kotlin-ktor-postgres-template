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
    suspend fun create(params: CreateUserParams): UserEntity

    suspend fun findById(id: UUID): UserEntity?

    suspend fun findByEmail(email: String): UserEntity?

    suspend fun update(params: UpdateUserParams): UserEntity

    suspend fun delete(id: UUID): Boolean

    // Querying
    suspend fun count(filter: UserFilter = UserFilter.Empty): Long

    suspend fun list(
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
