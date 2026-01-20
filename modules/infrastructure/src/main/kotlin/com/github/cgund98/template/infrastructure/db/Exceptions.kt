package com.github.cgund98.template.infrastructure.db

/** Base exception for repository operations. */
sealed class RepositoryException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/** Raised when an entity is not found in the repository. */
class NotFoundException(
    entityType: String,
    identifier: String? = null,
) : RepositoryException(
        if (identifier != null) {
            "$entityType not found with identifier: $identifier"
        } else {
            "$entityType not found"
        },
    )

/** Raised when attempting to create a duplicate entity. */
class DuplicateException(
    entityType: String,
    field: String,
    val value: String,
) : RepositoryException("$entityType with $field '$value' already exists")

/** Raised when a database operation fails unexpectedly. */
open class DatabaseException(
    message: String,
    cause: Throwable? = null,
) : RepositoryException(message, cause)

/** Raised when an update is attempted with no data. */
class NoFieldsToUpdateException : DatabaseException("No fields provided for update operation")
