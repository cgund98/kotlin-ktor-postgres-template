package com.github.cgund98.template.presentation.user

import io.ktor.server.plugins.requestvalidation.RequestValidationConfig
import io.ktor.server.plugins.requestvalidation.ValidationResult

fun RequestValidationConfig.userValidation() {
    validate<UserCreateRequest> { request ->
        if (request.email.isBlank()) {
            ValidationResult.Invalid("Email cannot be blank")
        } else if (!request.email.contains("@")) {
            ValidationResult.Invalid("Invalid email format")
        } else {
            ValidationResult.Valid
        }
    }
}
