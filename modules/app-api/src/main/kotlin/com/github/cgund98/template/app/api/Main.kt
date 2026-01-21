package com.github.cgund98.template.app.api

import com.github.cgund98.template.core.config.AppConfig
import com.github.cgund98.template.domain.user.userModule
import com.github.cgund98.template.infrastructure.db.FlywayMigrator
import com.github.cgund98.template.infrastructure.infrastructureModule
import com.github.cgund98.template.presentation.installStatusPages
import com.github.cgund98.template.presentation.user.userRoutes
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.koin.core.context.startKoin
import org.koin.ktor.plugin.Koin
import org.koin.ktor.plugin.koin

fun runMigrations() {
    // Start coin app just with infra
    val koinApp =
        startKoin {
            modules(
                infrastructureModule,
            )
        }

    // Run Flyway migrations on startup
    val flywayMigrator: FlywayMigrator = koinApp.koin.get()
    flywayMigrator.migrate()

    // Close when done
    koinApp.close()
}

fun main() {
    // Parse .env files with logging
    AppConfig.readEnvFiles()

    val logger = KotlinLogging.logger {}

    logger.info { "Running migrations" }

    runMigrations()

    logger.atInfo {
        message = "Starting webserver"
        payload = mapOf("port" to AppConfig.data.api.port)
    }

    embeddedServer(
        Netty,
        port = AppConfig.data.api.port,
        module = Application::module,
    ).start(wait = true)
}

suspend fun Application.module() {
    install(Koin) {
        modules(
            infrastructureModule,
            userModule,
        )
    }

    install(CallLogging) {
        disableDefaultColors()
    }
    install(RequestValidation)
    install(ContentNegotiation) {
        json()
    }

    installStatusPages()

    routing {
        swaggerUI(path = "docs", swaggerFile = AppConfig.data.api.openApiPath)

        get("/health") {
            call.respondText("Healthy")
        }

        userRoutes()
    }
}
