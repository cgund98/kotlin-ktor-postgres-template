package com.github.cgund98.template.app.worker

import com.github.cgund98.template.core.config.AppConfig
import com.github.cgund98.template.domain.user.handlers.UserCreatedHandler
import com.github.cgund98.template.domain.user.handlers.UserDeletedHandler
import com.github.cgund98.template.domain.user.handlers.UserUpdatedHandler
import com.github.cgund98.template.infrastructure.events.consumer.ConsumerSupervisor
import com.github.cgund98.template.infrastructure.events.consumer.HandlerBinding
import com.github.cgund98.template.infrastructure.events.consumer.SqsEventConsumer
import com.github.cgund98.template.infrastructure.events.consumer.SqsOptions
import com.github.cgund98.template.infrastructure.events.registry.user.UserCreated
import com.github.cgund98.template.infrastructure.events.registry.user.UserDeleted
import com.github.cgund98.template.infrastructure.events.registry.user.UserUpdated
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val workerModule =
    module {
        // Users
        singleOf(::UserCreatedHandler)
        singleOf(::UserUpdatedHandler)
        singleOf(::UserDeletedHandler)

        single(named("UserCreatedConsumer")) {
            SqsEventConsumer(
                sqsClient = get(),
                handlerBinding =
                    HandlerBinding(
                        handler = get<UserCreatedHandler>(),
                        eventType = UserCreated::class,
                    ),
                serializer = get(),
                sqsOptions =
                    SqsOptions(
                        queueUrl = AppConfig.data.events.queueUrlUserCreated,
                    ),
            )
        }

        single(named("UserUpdatedConsumer")) {
            SqsEventConsumer(
                sqsClient = get(),
                handlerBinding =
                    HandlerBinding(
                        handler = get<UserUpdatedHandler>(),
                        eventType = UserUpdated::class,
                    ),
                serializer = get(),
                sqsOptions =
                    SqsOptions(
                        queueUrl = AppConfig.data.events.queueUrlUserUpdated,
                    ),
            )
        }

        single(named("UserDeletedConsumer")) {
            SqsEventConsumer(
                sqsClient = get(),
                handlerBinding =
                    HandlerBinding(
                        handler = get<UserDeletedHandler>(),
                        eventType = UserDeleted::class,
                    ),
                serializer = get(),
                sqsOptions =
                    SqsOptions(
                        queueUrl = AppConfig.data.events.queueUrlUserDeleted,
                    ),
            )
        }

        single<ConsumerSupervisor> {
            ConsumerSupervisor(
                listOf(
                    get<SqsEventConsumer<UserCreated>>(named("UserCreatedConsumer")),
                    get<SqsEventConsumer<UserUpdated>>(named("UserUpdatedConsumer")),
                    get<SqsEventConsumer<UserDeleted>>(named("UserDeletedConsumer")),
                ),
            )
        }
    }
