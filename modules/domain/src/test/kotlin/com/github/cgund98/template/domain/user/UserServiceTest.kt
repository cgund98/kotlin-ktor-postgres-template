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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.UUID

class UserServiceTest :
    FunSpec({
        val userRepository = mockk<UserRepository>()
        val eventPublisher = mockk<EventPublisher>(relaxed = true)
        val txManager =
            object : TransactionManager {
                override suspend fun <T> withTransaction(block: suspend () -> T): T = block()
            }
        val userService = UserService(txManager, userRepository, eventPublisher)

        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        test("createUser should validate and create user") {
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

                result.id shouldBe userId
                result.email shouldBe email
                result.name shouldBe name
                result.age shouldBe age

                // Verify validation was called (implicitly by checking findByEmail)
                coVerify { userRepository.findByEmail(email) }

                val slot = slot<CreateUserParams>()
                coVerify { userRepository.create(capture(slot)) }
                slot.captured.email shouldBe email
                slot.captured.name shouldBe name
                slot.captured.age shouldBe age

                // Verify event was published
                val eventSlot = slot<UserCreated>()
                coVerify { eventPublisher.publish(capture(eventSlot)) }
                eventSlot.captured.payload.id shouldBe userId
                eventSlot.captured.payload.email shouldBe email
                eventSlot.captured.payload.name shouldBe name
                eventSlot.captured.payload.age shouldBe age
            }
        }

        test("updateUser should validate and update user") {
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

                result.id shouldBe id
                result.email shouldBe email
                result.name shouldBe name
                result.age shouldBe age

                // Verify validation calls
                coVerify { userRepository.findById(id) }
                coVerify { userRepository.findByEmail(email) }

                val slot = slot<UpdateUserParams>()
                coVerify { userRepository.update(capture(slot)) }
                slot.captured.id shouldBe id
                slot.captured.email shouldBe email
                slot.captured.name shouldBe name
                slot.captured.age shouldBe age

                // Verify event was published
                val eventSlot = slot<UserUpdated>()
                coVerify { eventPublisher.publish(capture(eventSlot)) }
                eventSlot.captured.payload.id shouldBe id
                eventSlot.captured.payload.email shouldBe email
                eventSlot.captured.payload.name shouldBe name
                eventSlot.captured.payload.age shouldBe age
            }
        }

        test("getUser should return user when found") {
            runTest {
                val id = UUID.randomUUID()
                val userEntity = UserEntity(id, "test@example.com", "Test User", 30, now, now)

                coEvery { userRepository.findById(id) } returns userEntity

                val result = userService.getUser(id)

                result shouldNotBe null
                result?.id shouldBe id
            }
        }

        test("getUser should return null when not found") {
            runTest {
                val id = UUID.randomUUID()

                coEvery { userRepository.findById(id) } returns null

                val result = userService.getUser(id)

                result shouldBe null
            }
        }

        test("listUsers should return list of users and count") {
            runTest {
                val page = PageParams(0, 10)
                val userEntity = UserEntity(UUID.randomUUID(), "test@example.com", "Test User", 30, now, now)
                val count = 1L

                coEvery { userRepository.count() } returns count
                coEvery { userRepository.list(page.size, page.offset) } returns listOf(userEntity)

                val (users, totalCount) = userService.listUsers(page)

                users.size shouldBe 1
                totalCount shouldBe count
                users[0].id shouldBe userEntity.id
            }
        }

        test("deleteUser should validate and delete user") {
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
                eventSlot.captured.payload.id shouldBe id
            }
        }
    })
