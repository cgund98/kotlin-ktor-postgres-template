plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "kotlin-ktor-postgres-template"

// Use the full colon-separated path
include(":modules:app-api")
include(":modules:app-worker")
include(":modules:domain")
include(":modules:infrastructure")
include(":modules:config")
include(":modules:observability")
