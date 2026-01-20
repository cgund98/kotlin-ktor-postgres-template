package com.github.cgund98.template.domain.user

import com.github.cgund98.template.domain.PageParams
import com.github.cgund98.template.domain.user.repo.CreateUserParams
import com.github.cgund98.template.domain.user.repo.UpdateUserParams
import com.github.cgund98.template.domain.user.repo.UserEntity
import com.github.cgund98.template.domain.user.repo.UserRepository
import com.github.cgund98.template.infrastructure.db.TransactionManager
import com.github.cgund98.template.infrastructure.events.publisher.EventPublisher
import com.github.cgund98.template.infrastructure.events.registry.user.UserCreated
import com.github.cgund98.template.infrastructure.events.registry.user.UserDeleted
import com.github.cgund98.template.infrastructure.events.registry.user.UserUpdated
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class UserServiceTest {
    private val userRepository = mockk<UserRepository>()
    private val eventPublisher = mockk<EventPublisher>(relaxed = true)
    private val txManager =
        object : TransactionManager {
            override suspend fun <T> withTransaction(block: suspend () -> T): T = block()
        }
    private val userService = UserService(txManager, userRepository, eventPublisher)

    private val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

    @Test
    fun `createUser should validate and create user`() =
        runTest {
            val email = "test@example.com"
            val name = "Test User"
            val age = 30
            val userId = UUID.randomUUID()
            val userEntity = UserEntity(userId, email, name, age, now, now)

            // Mock the validation dependencies
            coEvery { userRepository.findByEmail(email) } returns null
            coEvery { userRepository.create(any()) } returns userEntity
            coEvery { eventPublisher.publish(any<UserCreated>()) } returns Unit

            val result = userService.createUser(email, name, age)

            assertEquals(userId, result.id)
            assertEquals(email, result.email)
            assertEquals(name, result.name)
            assertEquals(age, result.age)

            // Verify validation was called (implicitly by checking findByEmail)
            coVerify { userRepository.findByEmail(email) }

            val slot = slot<CreateUserParams>()
            coVerify { userRepository.create(capture(slot)) }
            assertEquals(email, slot.captured.email)
            assertEquals(name, slot.captured.name)
            assertEquals(age, slot.captured.age)

            // Verify event was published
            val eventSlot = slot<UserCreated>()
            coVerify { eventPublisher.publish(capture(eventSlot)) }
            assertEquals(userId, eventSlot.captured.payload.id)
            assertEquals(email, eventSlot.captured.payload.email)
            assertEquals(name, eventSlot.captured.payload.name)
            assertEquals(age, eventSlot.captured.payload.age)
        }

    @Test
    fun `updateUser should validate and update user`() =
        runTest {
            val id = UUID.randomUUID()
            val email = "updated@example.com"
            val name = "Updated User"
            val age = 35
            val originalUser = UserEntity(id, "old@example.com", "Old Name", 20, now, now)
            val updatedUser = UserEntity(id, email, name, age, now, now)

            // Mock validation dependencies
            coEvery { userRepository.findById(id) } returns originalUser
            coEvery { userRepository.findByEmail(email) } returns null
            coEvery { userRepository.update(any()) } returns updatedUser
            coEvery { eventPublisher.publish(any<UserUpdated>()) } returns Unit

            val result = userService.updateUser(id, email, name, age)

            assertEquals(id, result.id)
            assertEquals(email, result.email)
            assertEquals(name, result.name)
            assertEquals(age, result.age)

            // Verify validation calls
            coVerify { userRepository.findById(id) }
            coVerify { userRepository.findByEmail(email) }

            val slot = slot<UpdateUserParams>()
            coVerify { userRepository.update(capture(slot)) }
            assertEquals(id, slot.captured.id)
            assertEquals(email, slot.captured.email)
            assertEquals(name, slot.captured.name)
            assertEquals(age, slot.captured.age)

            // Verify event was published
            val eventSlot = slot<UserUpdated>()
            coVerify { eventPublisher.publish(capture(eventSlot)) }
            assertEquals(id, eventSlot.captured.payload.id)
            assertEquals(email, eventSlot.captured.payload.email)
            assertEquals(name, eventSlot.captured.payload.name)
            assertEquals(age, eventSlot.captured.payload.age)
        }

    @Test
    fun `getUser should return user when found`() =
        runTest {
            val id = UUID.randomUUID()
            val userEntity = UserEntity(id, "test@example.com", "Test User", 30, now, now)

            coEvery { userRepository.findById(id) } returns userEntity

            val result = userService.getUser(id)

            assertNotNull(result)
            assertEquals(id, result?.id)
        }

    @Test
    fun `getUser should return null when not found`() =
        runTest {
            val id = UUID.randomUUID()

            coEvery { userRepository.findById(id) } returns null

            val result = userService.getUser(id)

            assertEquals(null, result)
        }

    @Test
    fun `listUsers should return list of users and count`() =
        runTest {
            val page = PageParams(0, 10)
            val userEntity = UserEntity(UUID.randomUUID(), "test@example.com", "Test User", 30, now, now)
            val count = 1L

            coEvery { userRepository.count() } returns count
            coEvery { userRepository.list(page.size, page.offset) } returns listOf(userEntity)

            val (users, totalCount) = userService.listUsers(page)

            assertEquals(1, users.size)
            assertEquals(count, totalCount)
            assertEquals(userEntity.id, users[0].id)
        }

    @Test
    fun `deleteUser should validate and delete user`() =
        runTest {
            val id = UUID.randomUUID()
            val userEntity = UserEntity(id, "test@example.com", "Test User", 30, now, now)

            // Mock validation dependencies
            coEvery { userRepository.findById(id) } returns userEntity
            coEvery { userRepository.delete(id) } returns true
            coEvery { eventPublisher.publish(any<UserDeleted>()) } returns Unit

            userService.deleteUser(id)

            coVerify { userRepository.findById(id) }
            coVerify { userRepository.delete(id) }

            // Verify event was published
            val eventSlot = slot<UserDeleted>()
            coVerify { eventPublisher.publish(capture(eventSlot)) }
            assertEquals(id, eventSlot.captured.payload.id)
        }
}
