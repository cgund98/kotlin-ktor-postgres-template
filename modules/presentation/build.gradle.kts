plugins {
    id("buildlogic.kotlin-library-conventions")
    id("io.ktor.plugin") version "3.3.3"
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.logging)

    // Core OpenAPI support
    implementation(libs.ktor.server.openapi)
    implementation(libs.ktor.server.swagger)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.ktor)

    implementation(project(":modules:domain"))
    implementation(project(":modules:infrastructure"))
}

// OpenAPI generation is experimental and can cause compiler compatibility issues.
ktor {
    @OptIn(io.ktor.plugin.OpenApiPreview::class)
    openApi {
        title = "Ktor Postgres Template"
        version = "0.1.0"
        summary = "This is a sample API"

        // Use 'target' to specify where the file goes
        target = project.layout.projectDirectory.file("../../openapi.json")
    }
}

// This ensures the JSON is generated before resources are packaged
tasks.processResources {
    // OpenAPI generation is experimental and tightly coupled to Kotlin compiler internals.
    // Keep it opt-in so normal builds/tests don't fail due to version mismatches.
    if (project.hasProperty("generateOpenApi")) {
        dependsOn("buildOpenApi")
    }
}
