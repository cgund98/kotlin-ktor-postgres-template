package com.github.cgund98.template.domain

/** Base exception for domain operations. */
sealed class DomainException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class ValidationException(
    message: String,
    field: String? = null,
) : DomainException(
        if (field != null) {
            "Validation failed for field '$field': $message"
        } else {
            "Validation failed: $message"
        },
    )

class DuplicateException(
    entityType: String,
    field: String,
    value: String,
) : DomainException("$entityType with $field: '$value' already exists")

class NotFoundException(
    entityType: String,
    identifierValue: String? = null,
    identifierField: String = "id",
) : DomainException("$entityType not found with $identifierField: '$identifierValue'")
