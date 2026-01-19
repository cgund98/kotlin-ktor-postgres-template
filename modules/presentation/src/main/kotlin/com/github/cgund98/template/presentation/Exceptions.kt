package com.github.cgund98.template.presentation

sealed class PresentationException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class MissingParameterException(
    field: String,
) : PresentationException("Missing required parameter: $field")

class InvalidParameterException(
    field: String,
    reason: String,
) : PresentationException("Invalid parameter: $field. Reason: $reason")
