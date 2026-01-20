package com.github.cgund98.template.domain.user.handlers

import com.github.cgund98.template.domain.user.UserService
import com.github.cgund98.template.infrastructure.events.consumer.EventHandler
import com.github.cgund98.template.infrastructure.events.registry.user.UserDeleted
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext

class UserDeletedHandler(
    private val service: UserService,
) : EventHandler<UserDeleted> {
    val logger =
        KotlinLogging.logger {}

    override suspend fun handleEvent(event: UserDeleted) {
        withLoggingContext("eventId" to event.id.toString()) {
            logger.info { "Handling UserDeleted event." }

            val user = service.getUser(event.payload.id)
            if (user == null) {
                logger.info { "User was confirmed to be deleted for UserDeleted event." }
                return
            }

            logger.atError {
                message = "Was able to fetch user for UserDeleted event."
                payload = mapOf("name" to user.name, "email" to user.email)
            }
        }
    }
}
