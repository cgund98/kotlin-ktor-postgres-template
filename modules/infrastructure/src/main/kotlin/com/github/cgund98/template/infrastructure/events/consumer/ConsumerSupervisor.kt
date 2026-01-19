package com.github.cgund98.template.infrastructure.events.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Suppress("TooGenericExceptionCaught")
class ConsumerSupervisor(
    private val consumers: List<EventConsumer>,
    private val backoff: Long = 5000,
) {
    private val logger = KotlinLogging.logger {}
    private val supervisorJob = SupervisorJob()

    // We create a specific scope for the consumers
    private val scope = CoroutineScope(Dispatchers.Default + supervisorJob)

    fun start() {
        logger.info { "Starting Supervisor for ${consumers.size} consumers" }

        consumers.forEach { consumer ->
            scope.launch {
                // This is our "Restart Strategy" loop
                while (isActive) {
                    try {
                        logger.info { "Starting consumer: ${consumer::class.simpleName}" }
                        consumer.start()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        logger.error(e) {
                            "Consumer ${consumer::class.simpleName} crashed! Restarting in 5s..."
                        }
                        delay(backoff) // Avoid tight-looping if the crash is persistent
                    }
                }
            }
        }
    }

    fun stop() {
        logger.info { "Shutting down supervisor..." }
        supervisorJob.cancel()
    }
}
