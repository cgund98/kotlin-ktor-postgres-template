package com.github.cgund98.template.presentation

import com.github.cgund98.template.domain.DomainException
import com.github.cgund98.template.domain.DuplicateException
import com.github.cgund98.template.domain.NotFoundException
import com.github.cgund98.template.domain.ValidationException
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.request.httpMethod
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable

private val logger = KotlinLogging.logger {}

fun Application.installStatusPages() {
    install(StatusPages) {
        configureDomainExceptions()
        configurePresentationExceptions()
        configureKtorExceptions()
        configureSystemExceptions()
    }
}

private fun StatusPagesConfig.configureDomainExceptions() {
    exception<DomainException> { call, cause ->
        val (status, type) = cause.toMapping()
        call.respond(status, ErrorResponse(status.value, cause.message ?: "Domain Error", type))
    }
}

private fun StatusPagesConfig.configurePresentationExceptions() {
    exception<PresentationException> { call, cause ->
        val (status, type) = cause.toMapping()
        call.respond(status, ErrorResponse(status.value, cause.message ?: "Presentation Error", type))
    }
}

private fun StatusPagesConfig.configureKtorExceptions() {
    exception<JsonConvertException> { call, _ ->
        call.respond(
            HttpStatusCode.BadRequest,
            ErrorResponse(
                HttpStatusCode.BadRequest.value,
                "The JSON provided does not match the expected format.",
                "INVALID_JSON",
            ),
        )
    }

    exception<BadRequestException> { call, cause ->
        val userFriendlyMessage = formatBadRequestMessage(cause)
        call.respond(
            HttpStatusCode.BadRequest,
            ErrorResponse(HttpStatusCode.BadRequest.value, userFriendlyMessage, "BAD_REQUEST"),
        )
    }
}

private fun StatusPagesConfig.configureSystemExceptions() {
    exception<Throwable> { call, cause ->
        logger.atError {
            message = "Request failed due to an uncaught exception"
            this.cause = cause // Automatically logs the stack trace

            // Adding structured metadata
            payload =
                mapOf(
                    "uri" to call.request.local.uri,
                    "method" to call.request.httpMethod.value,
                    "exception" to (cause::class.simpleName ?: "Unknown"),
                    "userAgent" to (call.request.headers["User-Agent"] ?: "unknown"),
                )
        }

        call.respond(
            HttpStatusCode.InternalServerError,
            ErrorResponse(
                status = 500,
                message = "An internal server error occurred.",
                type = "INTERNAL_SERVER_ERROR",
            ),
        )
    }
}

// --- Helpers ---

private fun formatBadRequestMessage(cause: BadRequestException): String {
    val message = cause.cause?.message ?: cause.message ?: "Invalid request format"
    return when {
        message.contains("Field '") -> message.substringBefore(" (")
        message.contains("non-nullable") -> "A required field is missing or null."
        else -> "The request body could not be parsed."
    }
}

private fun DomainException.toMapping() =
    when (this) {
        is NotFoundException -> HttpStatusCode.NotFound to "NOT_FOUND"
        is DuplicateException -> HttpStatusCode.Conflict to "ALREADY_EXISTS"
        is ValidationException -> HttpStatusCode.BadRequest to "BAD_REQUEST"
    }

private fun PresentationException.toMapping() =
    when (this) {
        is MissingParameterException -> HttpStatusCode.BadRequest to "BAD_REQUEST"
        is InvalidParameterException -> HttpStatusCode.BadRequest to "BAD_REQUEST"
    }

@Serializable
data class ErrorResponse(
    val status: Int,
    val message: String,
    val type: String,
)
