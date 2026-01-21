package com.github.cgund98.template.domain.user

import com.github.cgund98.template.domain.DuplicateException
import com.github.cgund98.template.domain.NotFoundException
import com.github.cgund98.template.domain.ValidationException
import com.github.cgund98.template.domain.user.repo.UserEntity
import com.github.cgund98.template.domain.user.repo.UserRepository
import io.mockk.every
import io.mockk.mockk
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
    private val userValidator = UserValidator(userRepository)
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

    // --- UserValidator Tests ---

    @Test
    fun `validateEmailNotDuplicate should pass if email is null or same as current`() {
        assertDoesNotThrow { userValidator.validateEmailNotDuplicate(null, "old@example.com") }
        assertDoesNotThrow {
            userValidator.validateEmailNotDuplicate(
                "old@example.com",
                "old@example.com",
            )
        }
    }

    @Test
    fun `validateEmailNotDuplicate should pass if email is unique`() {
        val newEmail = "new@example.com"
        every { userRepository.findByEmail(newEmail) } returns null

        assertDoesNotThrow { userValidator.validateEmailNotDuplicate(newEmail, "old@example.com") }
    }

    @Test
    fun `validateEmailNotDuplicate should throw if email exists`() {
        val newEmail = "existing@example.com"
        val existingUser = UserEntity(UUID.randomUUID(), newEmail, "Existing", 30, now, now)

        every { userRepository.findByEmail(newEmail) } returns existingUser

        val exception =
            assertThrows(DuplicateException::class.java) {
                userValidator.validateEmailNotDuplicate(newEmail, "old@example.com")
            }
        assertEquals("User with email: '$newEmail' already exists", exception.message)
    }

    @Test
    fun `validateCreateUser should pass for valid input`() {
        val email = "new@example.com"
        every { userRepository.findByEmail(email) } returns null

        assertDoesNotThrow { userValidator.validateCreateUser(email, "Name", 25) }
    }

    @Test
    fun `validateUpdateUser should pass for valid input`() {
        val id = UUID.randomUUID()
        val email = "new@example.com"
        val existingUser = UserEntity(id, "old@example.com", "Old Name", 20, now, now)

        every { userRepository.findById(id) } returns existingUser
        every { userRepository.findByEmail(email) } returns null

        assertDoesNotThrow { userValidator.validateUpdateUser(id, email, "New Name", 25) }
    }

    @Test
    fun `validateUpdateUser should pass with nullable parameters`() {
        val id = UUID.randomUUID()
        val existingUser = UserEntity(id, "old@example.com", "Old Name", 20, now, now)

        every { userRepository.findById(id) } returns existingUser
        every { userRepository.findByEmail("new@example.com") } returns null

        // Test with null values (partial update)
        assertDoesNotThrow { userValidator.validateUpdateUser(id, null, null, null) }
        assertDoesNotThrow { userValidator.validateUpdateUser(id, "new@example.com", null, null) }
        assertDoesNotThrow { userValidator.validateUpdateUser(id, null, "New Name", null) }
        assertDoesNotThrow { userValidator.validateUpdateUser(id, null, null, 30) }
    }

    @Test
    fun `validateUpdateUser should throw NotFoundException if user does not exist`() {
        val id = UUID.randomUUID()
        every { userRepository.findById(id) } returns null

        assertThrows(NotFoundException::class.java) {
            userValidator.validateUpdateUser(id, "email@example.com", "Name", 25)
        }
    }

    @Test
    fun `validateDeleteUser should pass if user exists`() {
        val id = UUID.randomUUID()
        val existingUser = UserEntity(id, "test@example.com", "Name", 20, now, now)

        every { userRepository.findById(id) } returns existingUser

        assertDoesNotThrow { userValidator.validateDeleteUser(id) }
    }

    @Test
    fun `validateDeleteUser should throw NotFoundException if user does not exist`() {
        val id = UUID.randomUUID()
        every { userRepository.findById(id) } returns null

        assertThrows(NotFoundException::class.java) {
            userValidator.validateDeleteUser(id)
        }
    }
}
