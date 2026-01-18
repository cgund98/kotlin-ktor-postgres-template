package com.github.cgund98.template.domain.user

import com.github.cgund98.template.domain.DuplicateException
import com.github.cgund98.template.domain.NotFoundException
import com.github.cgund98.template.domain.ValidationException
import com.github.cgund98.template.domain.user.repo.UserRepository
import java.util.UUID

const val MAX_AGE = 150

private val EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

/**
 * Pure Domain Validation Rules
 */
fun validateName(name: String?) {
    if (name == null) return

    if (name.isBlank()) {
        throw ValidationException("Name cannot be empty", field = "name")
    }
}

fun validateAge(age: Int?) {
    if (age == null) return

    if (age < 0) {
        throw ValidationException("Age cannot be negative", field = "age")
    }

    if (age > MAX_AGE) {
        throw ValidationException("Age cannot be greater than $MAX_AGE", field = "age")
    }
}

fun validateEmail(email: String?) {
    if (email == null) return

    if (email.isBlank()) {
        throw ValidationException("Email cannot be empty", field = "email")
    }

    // Validate with regex
    if (!email.matches(EMAIL_REGEX)) {
        throw ValidationException("Invalid email format", field = "email")
    }
}

/**
 * Extensions to UserRepository
 */
suspend fun UserRepository.validateEmailNotDuplicate(
    email: String?,
    currentEmail: String,
) {
    if (email == null || email == currentEmail) return

    // Since this is an extension function on UserRepository,
    // we can call 'findByEmail' directly.
    val existing = findByEmail(email)
    if (existing != null) {
        throw DuplicateException("User", "email", email)
    }
}

/**
 * Higher level compositions
 */

suspend fun UserRepository.validateCreateUser(
    email: String,
    name: String,
    age: Int?,
) {
    validateEmail(email)
    validateName(name)
    validateAge(age)
    validateEmailNotDuplicate(email, currentEmail = "")
}

suspend fun UserRepository.validateUpdateUser(
    id: UUID,
    email: String?,
    name: String?,
    age: Int?,
) {
    validateEmail(email)
    validateName(name)
    validateAge(age)

    val user = findById(id) ?: throw NotFoundException("User", id.toString())

    validateEmailNotDuplicate(email, currentEmail = user.email)
}

suspend fun UserRepository.validateDeleteUser(id: UUID) {
    findById(id) ?: throw NotFoundException("User", id.toString())
}
