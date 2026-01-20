package com.github.cgund98.template.domain.user.handlers

import com.github.cgund98.template.domain.user.UserService
import com.github.cgund98.template.infrastructure.events.consumer.EventHandler
import com.github.cgund98.template.infrastructure.events.registry.user.UserUpdated
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext

class UserUpdatedHandler(
    private val service: UserService,
) : EventHandler<UserUpdated> {
    val logger =
        KotlinLogging.logger {}

    override suspend fun handleEvent(event: UserUpdated) {
        withLoggingContext("eventId" to event.id.toString()) {
            logger.info { "Handling UserUpdated event." }

            val user = service.getUser(event.payload.id)
            if (user == null) {
                logger.warn { "User not found for UserUpdated event." }
                return
            }

            logger.atInfo {
                message = "Fetched user for UserUpdated event."
                payload = mapOf("name" to user.name, "email" to user.email)
            }
        }
    }
}
