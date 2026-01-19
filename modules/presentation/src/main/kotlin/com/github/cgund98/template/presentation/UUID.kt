package com.github.cgund98.template.presentation

import java.util.UUID

fun parseUUIDParameter(
    name: String,
    value: String,
): UUID {
    try {
        return UUID.fromString(value)
    } catch (_: IllegalArgumentException) {
        throw InvalidParameterException(name, "Invalid UUID format")
    }
}
