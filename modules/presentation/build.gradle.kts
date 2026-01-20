plugins {
    id("buildlogic.kotlin-library-conventions")
    id("buildlogic.linting-conventions")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.logging)

    // Core OpenAPI support
    implementation(libs.ktor.server.openapi)
    implementation(libs.ktor.server.swagger)
    implementation(libs.ktor.openapi.tools)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.ktor)

    implementation(project(":modules:domain"))
    implementation(project(":modules:infrastructure"))
}
