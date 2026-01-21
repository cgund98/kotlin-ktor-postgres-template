package com.github.cgund98.template.domain.user

import com.github.cgund98.template.domain.DuplicateException
import com.github.cgund98.template.domain.NotFoundException
import com.github.cgund98.template.domain.ValidationException
import com.github.cgund98.template.domain.user.repo.UserEntity
import com.github.cgund98.template.domain.user.repo.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.UUID

class UserValidatorTest :
    FunSpec({
        val userRepository = mockk<UserRepository>()
        val userValidator = UserValidator(userRepository)
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        // --- Pure Validation Rules ---

        test("validateName should pass for valid names") {
            validateName("John Doe")
            validateName(null) // Null is allowed (optional update)
        }

        test("validateName should throw for blank names") {
            val exception =
                shouldThrow<ValidationException> {
                    validateName("   ")
                }
            exception.message shouldBe "Validation failed for field 'name': Name cannot be empty"
        }

        test("validateAge should pass for valid ages") {
            validateAge(25)
            validateAge(0)
            validateAge(150)
            validateAge(null)
        }

        test("validateAge should throw for negative age") {
            val exception =
                shouldThrow<ValidationException> {
                    validateAge(-1)
                }
            exception.message shouldBe "Validation failed for field 'age': Age cannot be negative"
        }

        test("validateAge should throw for age greater than max") {
            val exception =
                shouldThrow<ValidationException> {
                    validateAge(151)
                }
            exception.message shouldBe "Validation failed for field 'age': Age cannot be greater than 150"
        }

        test("validateEmail should pass for valid emails") {
            validateEmail("test@example.com")
            validateEmail("user.name+tag@domain.co.uk")
            validateEmail(null)
        }

        test("validateEmail should throw for blank email") {
            val exception =
                shouldThrow<ValidationException> {
                    validateEmail("   ")
                }
            exception.message shouldBe "Validation failed for field 'email': Email cannot be empty"
        }

        test("validateEmail should throw for invalid format") {
            val invalidEmails =
                listOf("plainaddress", "#@%^%#$@#$@#.com", "@example.com", "Joe Smith <email@example.com>")
            invalidEmails.forEach { email ->
                val exception =
                    shouldThrow<ValidationException> {
                        validateEmail(email)
                    }
                exception.message shouldBe "Validation failed for field 'email': Invalid email format"
            }
        }

        // --- UserValidator Tests ---

        test("validateEmailNotDuplicate should pass if email is null or same as current") {
            runTest {
                userValidator.validateEmailNotDuplicate(null, "old@example.com")
                userValidator.validateEmailNotDuplicate(
                    "old@example.com",
                    "old@example.com",
                )
            }
        }

        test("validateEmailNotDuplicate should pass if email is unique") {
            runTest {
                val newEmail = "new@example.com"
                coEvery { userRepository.findByEmail(newEmail) } returns null

                userValidator.validateEmailNotDuplicate(newEmail, "old@example.com")
            }
        }

        test("validateEmailNotDuplicate should throw if email exists") {
            runTest {
                val newEmail = "existing@example.com"
                val existingUser = UserEntity(UUID.randomUUID(), newEmail, "Existing", 30, now, now)

                coEvery { userRepository.findByEmail(newEmail) } returns existingUser

                val exception =
                    shouldThrow<DuplicateException> {
                        userValidator.validateEmailNotDuplicate(newEmail, "old@example.com")
                    }
                exception.message shouldBe "User with email: '$newEmail' already exists"
            }
        }

        test("validateCreateUser should pass for valid input") {
            runTest {
                val email = "new@example.com"
                coEvery { userRepository.findByEmail(email) } returns null

                userValidator.validateCreateUser(email, "Name", 25)
            }
        }

        test("validateUpdateUser should pass for valid input") {
            runTest {
                val id = UUID.randomUUID()
                val email = "new@example.com"
                val existingUser = UserEntity(id, "old@example.com", "Old Name", 20, now, now)

                coEvery { userRepository.findById(id) } returns existingUser
                coEvery { userRepository.findByEmail(email) } returns null

                userValidator.validateUpdateUser(id, email, "New Name", 25)
            }
        }

        test("validateUpdateUser should pass with nullable parameters") {
            runTest {
                val id = UUID.randomUUID()
                val existingUser = UserEntity(id, "old@example.com", "Old Name", 20, now, now)

                coEvery { userRepository.findById(id) } returns existingUser
                coEvery { userRepository.findByEmail("new@example.com") } returns null

                // Test with null values (partial update)
                userValidator.validateUpdateUser(id, null, null, null)
                userValidator.validateUpdateUser(id, "new@example.com", null, null)
                userValidator.validateUpdateUser(id, null, "New Name", null)
                userValidator.validateUpdateUser(id, null, null, 30)
            }
        }

        test("validateUpdateUser should throw NotFoundException if user does not exist") {
            runTest {
                val id = UUID.randomUUID()
                coEvery { userRepository.findById(id) } returns null

                val exception =
                    shouldThrow<NotFoundException> {
                        userValidator.validateUpdateUser(id, "email@example.com", "Name", 25)
                    }
                exception.message shouldBe "User not found with id: '$id'"
            }
        }

        test("validateDeleteUser should pass if user exists") {
            runTest {
                val id = UUID.randomUUID()
                val existingUser = UserEntity(id, "test@example.com", "Name", 20, now, now)

                coEvery { userRepository.findById(id) } returns existingUser

                userValidator.validateDeleteUser(id)
            }
        }

        test("validateDeleteUser should throw NotFoundException if user does not exist") {
            runTest {
                val id = UUID.randomUUID()
                coEvery { userRepository.findById(id) } returns null

                val exception =
                    shouldThrow<NotFoundException> {
                        userValidator.validateDeleteUser(id)
                    }
                exception.message shouldBe "User not found with id: '$id'"
            }
        }
    })
