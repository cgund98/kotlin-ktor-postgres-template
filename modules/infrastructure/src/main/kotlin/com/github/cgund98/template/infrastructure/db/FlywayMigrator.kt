package com.github.cgund98.template.infrastructure.db

import io.github.oshai.kotlinlogging.KotlinLogging
import org.flywaydb.core.Flyway
import java.io.File
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}

/**
 * Runs Flyway migrations on application startup.
 */
class FlywayMigrator(
    private val dataSource: DataSource,
    private val migrationsPath: String,
) {
    fun migrate() {
        logger.info { "Running Flyway migrations from: $migrationsPath" }

        val migrationsDir = File(migrationsPath)
        if (!migrationsDir.exists()) {
            logger.warn { "Migrations directory does not exist: $migrationsPath" }
            return
        }

        val flyway =
            Flyway
                .configure()
                .dataSource(dataSource)
                .locations("filesystem:$migrationsPath")
                .baselineOnMigrate(true)
                .load()

        val info = flyway.info()
        val pending = info.pending()

        if (pending.isNotEmpty()) {
            logger.info { "Found ${pending.size} pending migration(s)" }
            pending.forEach { migration ->
                logger.info { "Pending: ${migration.version} - ${migration.description}" }
            }
        } else {
            logger.info { "No pending migrations" }
        }

        val result = flyway.migrate()
        logger.info {
            "Flyway migrations completed. Applied: ${result.migrationsExecuted}, " +
                "Schema version: ${result.targetSchemaVersion}"
        }
    }
}
