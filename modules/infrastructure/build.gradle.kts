import org.jooq.codegen.GenerationTool
import org.jooq.meta.jaxb.Configuration
import org.jooq.meta.jaxb.Database
import org.jooq.meta.jaxb.Generate
import org.jooq.meta.jaxb.Generator
import org.jooq.meta.jaxb.Jdbc
import org.jooq.meta.jaxb.Logging
import org.jooq.meta.jaxb.Target

plugins {
    id("buildlogic.kotlin-library-conventions")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.flyway)
    alias(libs.plugins.studer.jooq)
}

dependencies {
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.bundles.postgres)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.bundles.logging)

    api(libs.jooq)

    implementation(platform(libs.aws.bom))
    implementation(libs.aws.sns)
    implementation(libs.aws.sqs)

    // Flyway for runtime migrations
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)

    implementation(project(":modules:core"))

    testImplementation(libs.mockk)

    // jOOQ code generation dependencies
    jooqGenerator(libs.jooq.codegen)
    jooqGenerator(libs.jooq.meta)
    jooqGenerator(libs.postgresql.driver)

    // Testcontainers and Flyway for code generation task
    jooqGenerator(libs.testcontainers.postgresql)
    jooqGenerator("org.flywaydb:flyway-core:${libs.versions.flyway.get()}")
    jooqGenerator("org.flywaydb:flyway-database-postgresql:${libs.versions.flyway.get()}")
}

flyway {
    // Use environment variables or defaults for local development
    url = System.getenv("POSTGRES_URL") ?: "jdbc:postgresql://localhost:5432/app"
    user = System.getenv("POSTGRES_USER") ?: "postgres"
    password = System.getenv("POSTGRES_PASSWORD") ?: "postgres"
    locations = arrayOf("filesystem:${rootProject.projectDir}/resources/db/migrations")
    baselineOnMigrate = true
}

val skipJooqGeneration = project.findProperty("skipJooqGeneration")?.toString()?.toBoolean() == true

tasks.register<GenerateJooqFromTestcontainerTask>("generateJooqFromTestcontainer") {
    group = "jooq"
    description = "Generate jOOQ code from Testcontainer PostgreSQL after running Flyway migrations"

    codegenClasspath.from(configurations.getByName("jooqGenerator"))
    migrationsDir.set(
        project.rootProject.layout.projectDirectory
            .dir("resources/db/migrations"),
    )
    outputDir.set(layout.buildDirectory.dir("generated-src/jooq/main"))
    skipIfDockerUnavailable = skipJooqGeneration
}

jooq {
    version.set(libs.versions.jooq.get())
    edition.set(nu.studer.gradle.jooq.JooqEdition.OSS)

    configurations {
        create("main") {
            // Disable auto-generation - we use the Testcontainer task instead
            generateSchemaSourceOnCompilation.set(false)
        }
    }
}

// Add generated sources to source set (jOOQ generates Java code)
sourceSets.main {
    java.srcDirs(layout.buildDirectory.dir("generated-src/jooq/main"))
}

// Make compile depend on jOOQ generation (unless skipped)
if (!skipJooqGeneration) {
    tasks.named("compileKotlin") {
        dependsOn("generateJooqFromTestcontainer")
    }
} else {
    logger.warn("jOOQ code generation is skipped. Set -PskipJooqGeneration=false to enable.")
}
