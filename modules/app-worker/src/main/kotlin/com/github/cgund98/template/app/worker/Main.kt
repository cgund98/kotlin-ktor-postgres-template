package com.github.cgund98.template.app.worker

import com.github.cgund98.template.core.config.AppConfig
import com.github.cgund98.template.domain.user.userModule
import com.github.cgund98.template.infrastructure.events.consumer.ConsumerSupervisor
import com.github.cgund98.template.infrastructure.infrastructureModule
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin

fun main() {
    AppConfig.readEnvFiles()

    val logger = KotlinLogging.logger {}
    logger.info { "Starting Worker process..." }

    val koinApp =
        startKoin {
            modules(
                workerModule, // Consumers
                userModule, // Users domain
                infrastructureModule, // SQS, DB, Serializers
            )
        }

    // Supervisor
    val supervisor = koinApp.koin.get<ConsumerSupervisor>()

    // Add a Shutdown Hook for Docker/K8s graceful termination
    Runtime.getRuntime().addShutdownHook(
        Thread {
            logger.info { "Shutting down Worker process..." }
            supervisor.stop()
        },
    )

    // Start the supervisor loop
    // Since supervisor.start() launches coroutines, we need a runBlocking or similar
    runBlocking {
        supervisor.start()
        // Keep the main thread alive while coroutines run
        awaitCancellation()
    }
}
