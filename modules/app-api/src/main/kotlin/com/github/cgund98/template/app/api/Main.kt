package com.github.cgund98.template.app.api

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main() {
    println("Starting the Monolith...")

    System.setProperty("io.ktor.development", "true")

    embeddedServer(
        Netty,
        port = 8000,
        watchPaths = listOf("classes"),
        // Pass the function reference here instead of using { }
        module = Application::module,
    ).start(wait = true)
}

// Ensure this is a top-level function or a visible extension
suspend fun Application.module() {
    routing {
        get("/") {
            call.respondText("Hello from Kotlin Monolith! (Verified)")
        }
        get("/healthy") {
            call.respondText("Healthy")
        }
    }
}
