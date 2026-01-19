package com.github.cgund98.template.presentation

import com.github.cgund98.template.domain.DomainException
import com.github.cgund98.template.domain.DuplicateException
import com.github.cgund98.template.domain.NotFoundException
import com.github.cgund98.template.domain.ValidationException
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable

@Suppress("LongMethod")
fun Application.installStatusPages() {
    val logger = KotlinLogging.logger {}

    install(StatusPages) {
        // Catch all DomainExceptions
        exception<DomainException> { call, cause ->
            val (status, type) = cause.toMapping()

            call.respond(
                status,
                ErrorResponse(
                    status = status.value,
                    message = cause.message ?: "An unexpected error occurred",
                    type = type,
                ),
            )
        }

        // Catch all Presentation Exceptions
        exception<PresentationException> { call, cause ->

            val (status, type) = cause.toMapping()

            call.respond(
                status,
                ErrorResponse(
                    status = status.value,
                    message = cause.message ?: "An unexpected error occurred",
                    type = type,
                ),
            )
        }

        // Ktor exceptions
        exception<io.ktor.serialization.JsonConvertException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "status" to HttpStatusCode.BadRequest.value,
                    "error" to "Invalid Request Body",
                    "detail" to "The JSON provided does not match the expected format.",
                ),
            )
        }

        exception<io.ktor.server.plugins.BadRequestException> { call, cause ->
            // The 'cause' usually contains the technical reason (e.g., missing property)
            val message = cause.cause?.message ?: cause.message ?: "Invalid request format"

            // Clean up common technical phrases for the user
            val userFriendlyMessage =
                when {
                    message.contains("Field '") -> message.substringBefore(" (")

                    // Removes technical class info
                    message.contains("non-nullable") -> "A required field is missing or null."

                    else -> "The request body could not be parsed."
                }

            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    HttpStatusCode.BadRequest.value,
                    userFriendlyMessage,
                    "BAD_REQUEST",
                ),
            )
        }

        // Generic catch-all for non-domain errors (e.g., NullPointerException)
        exception<Throwable> { call, cause ->
            logger.atError {
                message = "Request threw uncaught exception"
                payload =
                    mapOf(
                        "errorMessage" to cause.message,
                        "errorType" to cause.toString(),
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
}

@Serializable
data class ErrorResponse(
    val status: Int,
    val message: String,
    val type: String,
)

private fun DomainException.toMapping(): Pair<HttpStatusCode, String> =
    when (this) {
        is NotFoundException -> HttpStatusCode.NotFound to "NOT_FOUND"
        is DuplicateException -> HttpStatusCode.Conflict to "ALREADY_EXISTS"
        is ValidationException -> HttpStatusCode.BadRequest to "BAD_REQUEST"
    }

private fun PresentationException.toMapping(): Pair<HttpStatusCode, String> =
    when (this) {
        is MissingParameterException -> HttpStatusCode.BadRequest to "BAD_REQUEST"
        is InvalidParameterException -> HttpStatusCode.BadRequest to "BAD_REQUEST"
    }
