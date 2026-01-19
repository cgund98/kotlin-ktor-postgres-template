package com.github.cgund98.template.domain.user.handlers

import com.github.cgund98.template.domain.user.UserService
import com.github.cgund98.template.infrastructure.events.consumer.EventHandler
import com.github.cgund98.template.infrastructure.events.registry.user.UserCreated
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext

class UserCreatedHandler(
    private val service: UserService,
) : EventHandler<UserCreated> {
    val logger =
        KotlinLogging.logger {}

    override suspend fun handleEvent(event: UserCreated) {
        withLoggingContext("eventId" to event.id.toString()) {
            logger.info { "Handling UserCreated event." }

            val user = service.getUser(event.payload.id)
            if (user == null) {
                logger.warn { "User not found for UserCreated event." }
                return
            }

            logger.atInfo {
                message = "Fetched user for UserCreated event."
                payload = mapOf("name" to user.name, "email" to user.email)
            }
        }
    }
}
