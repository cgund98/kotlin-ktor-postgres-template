package com.github.cgund98.template.domain.test

import com.github.cgund98.template.domain.user.userModule
import com.github.cgund98.template.infrastructure.DatabaseConfig
import com.github.cgund98.template.infrastructure.db.FlywayMigrator
import com.github.cgund98.template.infrastructure.infrastructureModule
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * Singleton Postgres testcontainer that can be reused across tests.
 * The container is started once and stopped when the JVM exits.
 */
object PostgresTestContainer {
    private val container: PostgreSQLContainer<*> =
        PostgreSQLContainer(DockerImageName.parse("postgres:17"))
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test")
            .apply {
                // Start container once
                start()
                logger.info { "Postgres testcontainer started: $jdbcUrl" }
            }

    val jdbcUrl: String
        get() = container.jdbcUrl

    val username: String
        get() = container.username

    val password: String
        get() = container.password

    fun stop() {
        container.stop()
        logger.info { "Postgres testcontainer stopped" }
    }
}

/**
 * Creates a test Koin module with the testcontainer database configuration.
 */
fun createTestInfrastructureModule(migrationsPath: String): org.koin.core.module.Module {
    val databaseConfig =
        DatabaseConfig(
            url = PostgresTestContainer.jdbcUrl,
            user = PostgresTestContainer.username,
            password = PostgresTestContainer.password,
            maxPoolSize = 5, // Smaller pool for tests
            connectionTimeout = 10000,
            leakDetectionThreshold = 1000,
        )

    return infrastructureModule(databaseConfig, migrationsPath)
}

/**
 * Sets up Koin with test modules and runs migrations.
 * Additional modules can be provided to override defaults (e.g., mock EventPublisher).
 * Returns a function to stop Koin when done.
 */
fun setupTestKoin(
    migrationsPath: String = getMigrationsPath(),
    additionalModules: List<org.koin.core.module.Module> = emptyList(),
): () -> Unit {
    logger.info { "Setting up test Koin with migrations from: $migrationsPath" }

    val koinApp =
        startKoin {
            modules(
                createTestInfrastructureModule(migrationsPath),
                userModule,
                *additionalModules.toTypedArray(),
            )
        }

    // Run migrations
    val flywayMigrator: FlywayMigrator = koinApp.koin.get()
    flywayMigrator.migrate()

    logger.info { "Test Koin setup complete and migrations applied" }

    return {
        stopKoin()
        logger.info { "Test Koin stopped" }
    }
}

/**
 * Gets the migrations path relative to the project root.
 * Works whether running from project root or from a module directory.
 */
private fun getMigrationsPath(): String {
    val userDir = System.getProperty("user.dir")
    val currentDir = File(userDir)

    // Try different possible locations
    val possiblePaths =
        listOf(
            File(currentDir, "resources/db/migrations"), // Running from project root
            File(currentDir.parentFile, "resources/db/migrations"), // Running from module
            File(currentDir.parentFile?.parentFile, "resources/db/migrations"), // Nested
        )

    for (path in possiblePaths) {
        if (path?.exists() == true) {
            return path.absolutePath
        }
    }

    throw IllegalStateException(
        "Migrations directory not found. Tried: ${possiblePaths.mapNotNull { it?.absolutePath }.joinToString(", ")}. " +
            "Current directory: ${currentDir.absolutePath}",
    )
}
