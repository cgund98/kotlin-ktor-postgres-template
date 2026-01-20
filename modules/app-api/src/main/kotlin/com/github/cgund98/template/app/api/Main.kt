package com.github.cgund98.template.app.api

import com.github.cgund98.template.core.config.AppConfig
import com.github.cgund98.template.domain.user.userModule
import com.github.cgund98.template.infrastructure.infrastructureModule
import com.github.cgund98.template.presentation.installStatusPages
import com.github.cgund98.template.presentation.user.userRoutes
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smiley4.ktoropenapi.OpenApi
import io.github.smiley4.ktoropenapi.openApi
import io.github.smiley4.ktoropenapi.route
import io.github.smiley4.ktorswaggerui.swaggerUI
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.koin.ktor.plugin.Koin

fun main() {
    System.setProperty("io.ktor.development", "true")

    // Parse .env files with logging
    AppConfig.readEnvFiles()

    val logger = KotlinLogging.logger {}

    logger.atInfo {
        message = "Starting webserver"
        payload = mapOf("port" to AppConfig.data.api.port)
    }

    embeddedServer(
        Netty,
        port = AppConfig.data.api.port,
        watchPaths = listOf("classes"),
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

    install(OpenApi) {
        info {
            title = "Ktor Postgres Template"
            version = "0.1.0"
            description = "This is a sample API"
        }
    }

    installStatusPages()

    routing {


        get("/health") {
            call.respondText("Healthy")
        }

        route("openapi.json") {
            // Create a route to expose the OpenAPI specification file at `/api.json`.
            openApi()
        }

        route("/docs") {
            swaggerUI("../openapi.json")
        }

        userRoutes()
    }
}
