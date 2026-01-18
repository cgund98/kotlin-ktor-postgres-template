package com.github.cgund98.template.domain.user

import com.github.cgund98.template.domain.DuplicateException
import com.github.cgund98.template.domain.NotFoundException
import com.github.cgund98.template.domain.ValidationException
import com.github.cgund98.template.domain.user.repo.UserEntity
import com.github.cgund98.template.domain.user.repo.UserRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID

class UserValidatorTest {
    private val userRepository = mockk<UserRepository>()
    private val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

    // --- Pure Validation Rules ---

    @Test
    fun `validateName should pass for valid names`() {
        assertDoesNotThrow { validateName("John Doe") }
        assertDoesNotThrow { validateName(null) } // Null is allowed (optional update)
    }

    @Test
    fun `validateName should throw for blank names`() {
        val exception =
            assertThrows(ValidationException::class.java) {
                validateName("   ")
            }
        assertEquals("Validation failed for field 'name': Name cannot be empty", exception.message)
    }

    @Test
    fun `validateAge should pass for valid ages`() {
        assertDoesNotThrow { validateAge(25) }
        assertDoesNotThrow { validateAge(0) }
        assertDoesNotThrow { validateAge(150) }
        assertDoesNotThrow { validateAge(null) }
    }

    @Test
    fun `validateAge should throw for negative age`() {
        val exception =
            assertThrows(ValidationException::class.java) {
                validateAge(-1)
            }
        assertEquals("Validation failed for field 'age': Age cannot be negative", exception.message)
    }

    @Test
    fun `validateAge should throw for age greater than max`() {
        val exception =
            assertThrows(ValidationException::class.java) {
                validateAge(151)
            }
        assertEquals("Validation failed for field 'age': Age cannot be greater than 150", exception.message)
    }

    @Test
    fun `validateEmail should pass for valid emails`() {
        assertDoesNotThrow { validateEmail("test@example.com") }
        assertDoesNotThrow { validateEmail("user.name+tag@domain.co.uk") }
        assertDoesNotThrow { validateEmail(null) }
    }

    @Test
    fun `validateEmail should throw for blank email`() {
        val exception =
            assertThrows(ValidationException::class.java) {
                validateEmail("   ")
            }
        assertEquals("Validation failed for field 'email': Email cannot be empty", exception.message)
    }

    @Test
    fun `validateEmail should throw for invalid format`() {
        val invalidEmails = listOf("plainaddress", "#@%^%#$@#$@#.com", "@example.com", "Joe Smith <email@example.com>")
        invalidEmails.forEach { email ->
            val exception =
                assertThrows(ValidationException::class.java) {
                    validateEmail(email)
                }
            assertEquals("Validation failed for field 'email': Invalid email format", exception.message)
        }
    }

    // --- Repository Extension Tests ---

    @Test
    fun `validateEmailNotDuplicate should pass if email is null or same as current`() =
        runTest {
            assertDoesNotThrow { runBlocking { userRepository.validateEmailNotDuplicate(null, "old@example.com") } }
            assertDoesNotThrow {
                runBlocking {
                    userRepository.validateEmailNotDuplicate(
                        "old@example.com",
                        "old@example.com",
                    )
                }
            }
        }

    @Test
    fun `validateEmailNotDuplicate should pass if email is unique`() =
        runTest {
            val newEmail = "new@example.com"
            coEvery { userRepository.findByEmail(newEmail) } returns null

            assertDoesNotThrow { runBlocking { userRepository.validateEmailNotDuplicate(newEmail, "old@example.com") } }
        }

    @Test
    fun `validateEmailNotDuplicate should throw if email exists`() =
        runTest {
            val newEmail = "existing@example.com"
            val existingUser = UserEntity(UUID.randomUUID(), newEmail, "Existing", 30, now, now)

            coEvery { userRepository.findByEmail(newEmail) } returns existingUser

            val exception =
                assertThrows(DuplicateException::class.java) {
                    runBlocking { userRepository.validateEmailNotDuplicate(newEmail, "old@example.com") }
                }
            assertEquals("User with email: '$newEmail' already exists", exception.message)
        }

    @Test
    fun `validateCreateUser should pass for valid input`() =
        runTest {
            val email = "new@example.com"
            coEvery { userRepository.findByEmail(email) } returns null

            assertDoesNotThrow { runBlocking { userRepository.validateCreateUser(email, "Name", 25) } }
        }

    @Test
    fun `validateUpdateUser should pass for valid input`() =
        runTest {
            val id = UUID.randomUUID()
            val email = "new@example.com"
            val existingUser = UserEntity(id, "old@example.com", "Old Name", 20, now, now)

            coEvery { userRepository.findById(id) } returns existingUser
            coEvery { userRepository.findByEmail(email) } returns null

            assertDoesNotThrow { runBlocking { userRepository.validateUpdateUser(id, email, "New Name", 25) } }
        }

    @Test
    fun `validateUpdateUser should throw NotFoundException if user does not exist`() =
        runTest {
            val id = UUID.randomUUID()
            coEvery { userRepository.findById(id) } returns null

            assertThrows(NotFoundException::class.java) {
                runBlocking { userRepository.validateUpdateUser(id, "email@example.com", "Name", 25) }
            }
        }

    @Test
    fun `validateDeleteUser should pass if user exists`() =
        runTest {
            val id = UUID.randomUUID()
            val existingUser = UserEntity(id, "test@example.com", "Name", 20, now, now)

            coEvery { userRepository.findById(id) } returns existingUser

            assertDoesNotThrow { runBlocking { userRepository.validateDeleteUser(id) } }
        }

    @Test
    fun `validateDeleteUser should throw NotFoundException if user does not exist`() =
        runTest {
            val id = UUID.randomUUID()
            coEvery { userRepository.findById(id) } returns null

            assertThrows(NotFoundException::class.java) {
                runBlocking { userRepository.validateDeleteUser(id) }
            }
        }
}
