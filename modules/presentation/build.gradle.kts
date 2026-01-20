plugins {
    id("buildlogic.kotlin-library-conventions")
    id("buildlogic.linting-conventions")
    id("io.ktor.plugin") version "3.3.3"
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.logging)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Core OpenAPI support
    implementation("io.ktor:ktor-server-openapi:3.3.3")
    implementation("io.ktor:ktor-server-swagger:3.3.3")

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.ktor)

    implementation(project(":modules:domain"))
    implementation(project(":modules:infrastructure"))
}

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
    dependsOn("buildOpenApi")
}
