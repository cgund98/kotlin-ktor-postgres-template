import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jooq.codegen.GenerationTool
import org.jooq.meta.jaxb.Configuration
import org.jooq.meta.jaxb.Database
import org.jooq.meta.jaxb.Generate
import org.jooq.meta.jaxb.Generator
import org.jooq.meta.jaxb.Jdbc
import org.jooq.meta.jaxb.Logging
import org.jooq.meta.jaxb.Target
import java.net.URLClassLoader

abstract class GenerateJooqFromTestcontainerTask : DefaultTask() {
    @get:InputFiles
    abstract val codegenClasspath: ConfigurableFileCollection

    @get:InputDirectory
    abstract val migrationsDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:org.gradle.api.tasks.Optional
    @get:org.gradle.api.tasks.Input
    var skipIfDockerUnavailable: Boolean? = false

    @TaskAction
    fun generate() {
        val outputDirFile = outputDir.get().asFile
        val migrationsDirFile = migrationsDir.get().asFile

        // Create classloader with all dependencies
        val urls = codegenClasspath.files.map { it.toURI().toURL() }.toTypedArray()
        val classLoader = URLClassLoader(urls, Thread.currentThread().contextClassLoader)

        // Save current classloader and switch
        val originalClassLoader = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = classLoader

        try {
            // Load classes using reflection
            val postgresContainerClass = classLoader.loadClass("org.testcontainers.containers.PostgreSQLContainer")
            val flywayClass = classLoader.loadClass("org.flywaydb.core.Flyway")

            // Start container
            @Suppress("UNCHECKED_CAST")
            val container =
                try {
                    val instance =
                        postgresContainerClass
                            .getConstructor(String::class.java)
                            .newInstance("postgres:17") as Any

                    @Suppress("UNCHECKED_CAST")
                    postgresContainerClass
                        .getMethod(
                            "withDatabaseName",
                            String::class.java,
                        ).invoke(instance, "app") as Any
                    @Suppress("UNCHECKED_CAST")
                    postgresContainerClass
                        .getMethod(
                            "withUsername",
                            String::class.java,
                        ).invoke(instance, "postgres") as Any
                    @Suppress("UNCHECKED_CAST")
                    postgresContainerClass
                        .getMethod(
                            "withPassword",
                            String::class.java,
                        ).invoke(instance, "postgres") as Any

                    // Try to start - this will fail if Docker isn't available
                    postgresContainerClass.getMethod("start").invoke(instance)
                    instance
                } catch (e: Exception) {
                    val cause = e.cause ?: e
                    if (skipIfDockerUnavailable == true) {
                        logger.warn(
                            """
                        |Skipping jOOQ code generation: Docker/OrbStack not available.
                        |Error: ${cause.message}
                        |
                        |To generate jOOQ code manually, ensure Docker/OrbStack is running and run:
                        |  ./gradlew :modules:infrastructure:generateJooqFromTestcontainer
                        |
                        |Or set -PskipJooqGeneration=true to skip this step entirely.
                            """.trimMargin(),
                        )
                        return
                    } else {
                        throw org.gradle.api.GradleException(
                            """
                        |Failed to start Testcontainer for jOOQ code generation.
                        |Error: ${cause.message}
                        |
                        |This usually means Docker/OrbStack is not running or not accessible.
                        |Please ensure Docker/OrbStack is running and try again.
                        |
                        |To skip code generation, set -PskipJooqGeneration=true
                            """.trimMargin(),
                            e,
                        )
                    }
                }

            try {
                val jdbcUrl =
                    try {
                        postgresContainerClass.getMethod("getJdbcUrl").invoke(container) as String
                    } catch (e: java.lang.reflect.InvocationTargetException) {
                        throw org.gradle.api.GradleException(
                            "Failed to get JDBC URL: ${e.targetException?.message ?: e.message}",
                            e.targetException ?: e,
                        )
                    }
                val username =
                    try {
                        postgresContainerClass.getMethod("getUsername").invoke(container) as String
                    } catch (e: java.lang.reflect.InvocationTargetException) {
                        throw org.gradle.api.GradleException(
                            "Failed to get username: ${e.targetException?.message ?: e.message}",
                            e.targetException ?: e,
                        )
                    }
                val dbPassword =
                    try {
                        postgresContainerClass.getMethod("getPassword").invoke(container) as String
                    } catch (e: java.lang.reflect.InvocationTargetException) {
                        throw org.gradle.api.GradleException(
                            "Failed to get password: ${e.targetException?.message ?: e.message}",
                            e.targetException ?: e,
                        )
                    }

                logger.info("Container started: $jdbcUrl")
                logger.info("Running Flyway migrations from: ${migrationsDirFile.absolutePath}")

                // Run Flyway migrations
                try {
                    // First, ensure PostgreSQL driver is loaded
                    try {
                        classLoader.loadClass("org.postgresql.Driver")
                        logger.info("PostgreSQL driver class loaded successfully")
                    } catch (e: ClassNotFoundException) {
                        throw org.gradle.api.GradleException(
                            "PostgreSQL driver not found in classpath. Make sure postgresql-driver is in jooqGenerator configuration.",
                            e,
                        )
                    }

                    // Load Flyway database plugin class to trigger ServiceLoader registration
                    try {
                        classLoader.loadClass("org.flywaydb.database.postgresql.PostgreSQLDatabase")
                        logger.info("Flyway PostgreSQL database plugin loaded successfully")
                    } catch (e: ClassNotFoundException) {
                        logger.warn("Flyway PostgreSQL database plugin class not found, but continuing...")
                    }

                    val flywayConfigure = flywayClass.getMethod("configure")

                    @Suppress("UNCHECKED_CAST")
                    val flywayBuilder = flywayConfigure.invoke(null) as Any
                    val builderClass: Class<*> = flywayBuilder.javaClass

                    // Try to set driver explicitly
                    try {
                        val driverMethod = builderClass.getMethod("driver", String::class.java)
                        driverMethod.invoke(flywayBuilder, "org.postgresql.Driver")
                        logger.info("Set Flyway driver explicitly")
                    } catch (e: NoSuchMethodException) {
                        logger.info("Driver method not available, Flyway will auto-detect")
                    } catch (e: java.lang.reflect.InvocationTargetException) {
                        logger.warn("Failed to set driver explicitly: ${e.targetException?.message ?: e.message}")
                    }

                    val dataSource =
                        builderClass.getMethod(
                            "dataSource",
                            String::class.java,
                            String::class.java,
                            String::class.java,
                        )
                    val locations = builderClass.getMethod("locations", Array<String>::class.java)
                    val baselineOnMigrate = builderClass.getMethod("baselineOnMigrate", Boolean::class.java)
                    val load = builderClass.getMethod("load")
                    val migrateMethod = flywayClass.getMethod("migrate")

                    // Configure builder: dataSource -> locations -> baseline -> load
                    @Suppress("UNCHECKED_CAST")
                    val dataSourceResult =
                        try {
                            dataSource.invoke(flywayBuilder, jdbcUrl, username, dbPassword)
                        } catch (e: java.lang.reflect.InvocationTargetException) {
                            val cause = e.targetException ?: e
                            // If it's the "No database found" error, provide more helpful message
                            if (cause.message?.contains("No database found") == true) {
                                throw org.gradle.api.GradleException(
                                    """
                                |Flyway cannot find PostgreSQL database plugin.
                                |This usually means flyway-database-postgresql is not on the classpath or ServiceLoader cannot find it.
                                |
                                |Error: ${cause.message}
                                |
                                |Please verify that:
                                |1. flyway-database-postgresql is in jooqGenerator dependencies
                                |2. The JAR is actually included in the classpath
                                    """.trimMargin(),
                                    cause,
                                )
                            }
                            throw org.gradle.api.GradleException(
                                "Failed to configure Flyway dataSource: ${cause.message}",
                                cause,
                            )
                        }

                    @Suppress("UNCHECKED_CAST")
                    val locationsResult =
                        try {
                            locations.invoke(dataSourceResult, arrayOf("filesystem:${migrationsDirFile.absolutePath}"))
                        } catch (e: java.lang.reflect.InvocationTargetException) {
                            throw org.gradle.api.GradleException(
                                "Failed to configure Flyway locations: ${e.targetException?.message ?: e.message}",
                                e.targetException ?: e,
                            )
                        }

                    @Suppress("UNCHECKED_CAST")
                    val configuredFlyway =
                        try {
                            baselineOnMigrate.invoke(locationsResult, true) as Any
                        } catch (e: java.lang.reflect.InvocationTargetException) {
                            throw org.gradle.api.GradleException(
                                "Failed to configure Flyway baseline: ${e.targetException?.message ?: e.message}",
                                e.targetException ?: e,
                            )
                        }

                    @Suppress("UNCHECKED_CAST")
                    val flywayInstance =
                        try {
                            load.invoke(configuredFlyway) as Any
                        } catch (e: java.lang.reflect.InvocationTargetException) {
                            throw org.gradle.api.GradleException(
                                "Failed to load Flyway: ${e.targetException?.message ?: e.message}",
                                e.targetException ?: e,
                            )
                        }

                    try {
                        migrateMethod.invoke(flywayInstance)
                    } catch (e: java.lang.reflect.InvocationTargetException) {
                        throw org.gradle.api.GradleException(
                            "Failed to run Flyway migrate: ${e.targetException?.message ?: e.message}",
                            e.targetException ?: e,
                        )
                    }
                    logger.info("Flyway migrations completed successfully")
                } catch (e: java.lang.reflect.InvocationTargetException) {
                    val cause = e.targetException ?: e.cause ?: e
                    logger.error("Flyway migration failed: ${cause.message}", cause)
                    cause.printStackTrace()
                    throw org.gradle.api.GradleException("Failed to run Flyway migrations: ${cause.message}", cause)
                } catch (e: Exception) {
                    val cause = e.cause ?: e
                    logger.error("Flyway migration failed: ${cause.message}", cause)
                    cause.printStackTrace()
                    throw org.gradle.api.GradleException("Failed to run Flyway migrations: ${cause.message}", e)
                }

                logger.info("Generating jOOQ code...")

                // Configure jOOQ code generation
                val jooqConfig =
                    Configuration().apply {
                        logging = Logging.WARN
                        jdbc =
                            Jdbc().apply {
                                driver = "org.postgresql.Driver"
                                url = jdbcUrl
                                user = username
                                password = dbPassword
                            }
                        generator =
                            Generator().apply {
                                database =
                                    Database().apply {
                                        name = "org.jooq.meta.postgres.PostgresDatabase"
                                        inputSchema = "public"
                                        includes = ".*"
                                        excludes = "flyway_schema_history"
                                    }
                                generate =
                                    Generate().apply {
                                        isDeprecated = false
                                        isRecords = true
                                        isFluentSetters = true
                                        isPojos = true
                                    }
                                target =
                                    Target().apply {
                                        packageName = "com.github.cgund98.template.infrastructure.jooq.generated"
                                        directory = outputDirFile.absolutePath
                                    }
                            }
                    }

                // Generate jOOQ code
                try {
                    GenerationTool.generate(jooqConfig)
                    logger.info("jOOQ code generation completed successfully")
                    logger.info("Generated files in: ${outputDirFile.absolutePath}")
                } catch (e: Exception) {
                    logger.error("jOOQ code generation failed: ${e.message}", e)
                    e.printStackTrace()
                    throw org.gradle.api.GradleException("Failed to generate jOOQ code: ${e.message}", e)
                }
            } finally {
                postgresContainerClass.getMethod("stop").invoke(container)
            }
        } finally {
            Thread.currentThread().contextClassLoader = originalClassLoader
        }
    }
}
