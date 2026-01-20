package com.github.cgund98.template.domain.user

import com.github.cgund98.template.domain.PageParams
import com.github.cgund98.template.domain.user.repo.CreateUserParams
import com.github.cgund98.template.domain.user.repo.UpdateUserParams
import com.github.cgund98.template.domain.user.repo.UserRepository
import com.github.cgund98.template.infrastructure.db.TransactionManager
import com.github.cgund98.template.infrastructure.events.publisher.EventPublisher
import com.github.cgund98.template.infrastructure.events.registry.user.UserCreated
import com.github.cgund98.template.infrastructure.events.registry.user.UserDeleted
import com.github.cgund98.template.infrastructure.events.registry.user.UserDeletedPayload
import com.github.cgund98.template.infrastructure.events.registry.user.UserUpdated
import java.util.UUID

class UserService(
    private val txManager: TransactionManager,
    private val userRepository: UserRepository,
    private val publisher: EventPublisher,
) {
    private val userValidator = UserValidator(userRepository)

    suspend fun createUser(
        email: String,
        name: String,
        age: Int? = null,
    ): User {
        val createdUser =
            txManager.withTransaction {
                userValidator.validateCreateUser(email, name, age)

                val params = CreateUserParams(email = email, name = name, age = age)
                val entity = userRepository.create(params)

                entity.toDomain()
            }

        // Publish an event
        val payload = createdUser.toCreatedPayload()
        val event = UserCreated.create(payload)

        publisher.publish(event)

        return createdUser
    }

    suspend fun updateUser(
        id: UUID,
        email: String?,
        name: String?,
        age: Int?,
    ): User {
        val updatedUser =
            txManager.withTransaction {
                userValidator.validateUpdateUser(id, email, name, age)

                val params = UpdateUserParams(id = id, email = email, name = name, age = age)
                val entity = userRepository.update(params)

                entity.toDomain()
            }

        // Publish an event
        val payload = updatedUser.toUpdatedPayload()
        val event = UserUpdated.create(payload)

        publisher.publish(event)

        return updatedUser
    }

    suspend fun getUser(id: UUID): User? =
        txManager.withTransaction {
            val entity = userRepository.findById(id)
            entity?.toDomain()
        }

    suspend fun listUsers(page: PageParams): Pair<List<User>, Long> =
        txManager.withTransaction {
            val count = userRepository.count()
            val users = userRepository.list(limit = page.size, offset = page.offset)
            users.map { it.toDomain() } to count
        }

    suspend fun deleteUser(id: UUID) {
        txManager.withTransaction {
            userValidator.validateDeleteUser(id)

            userRepository.delete(id)
        }

        // Publish an event
        val payload = UserDeletedPayload(id = id)
        val event = UserDeleted.create(payload)

        publisher.publish(event)
    }
}
