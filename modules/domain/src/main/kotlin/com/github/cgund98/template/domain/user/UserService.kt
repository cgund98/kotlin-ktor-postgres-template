package com.github.cgund98.template.domain.user

import com.github.cgund98.template.domain.PageParams
import com.github.cgund98.template.domain.user.repo.CreateUserParams
import com.github.cgund98.template.domain.user.repo.UpdateUserParams
import com.github.cgund98.template.domain.user.repo.UserRepository
import com.github.cgund98.template.infrastructure.db.TransactionManager
import java.util.UUID

class UserService(
    private val txManager: TransactionManager,
    private val userRepository: UserRepository,
) {
    private val userValidator = UserValidator(userRepository)

    suspend fun createUser(
        email: String,
        name: String,
        age: Int? = null,
    ): User =
        txManager.withTransaction {
            userValidator.validateCreateUser(email, name, age)

            val params = CreateUserParams(email = email, name = name, age = age)
            val entity = userRepository.create(params)

            User.fromEntity(entity)
        }

    suspend fun updateUser(
        id: UUID,
        email: String?,
        name: String?,
        age: Int?,
    ): User =
        txManager.withTransaction {
            userValidator.validateUpdateUser(id, email, name, age)

            val params = UpdateUserParams(id = id, email = email, name = name, age = age)
            val entity = userRepository.update(params)

            User.fromEntity(entity)
        }

    suspend fun getUser(id: UUID): User? =
        txManager.withTransaction {
            val entity = userRepository.findById(id)
            entity?.let { User.fromEntity(it) }
        }

    suspend fun listUsers(page: PageParams): Pair<List<User>, Long> =
        txManager.withTransaction {
            val count = userRepository.count()
            val users = userRepository.list(limit = page.size, offset = page.offset)
            users.map { User.fromEntity(it) } to count
        }

    suspend fun deleteUser(id: UUID): Boolean =
        txManager.withTransaction {
            userValidator.validateDeleteUser(id)

            userRepository.delete(id)
        }
}
