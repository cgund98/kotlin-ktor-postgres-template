package com.github.cgund98.template.infrastructure

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.sns.SnsClient
import aws.sdk.kotlin.services.sqs.SqsClient
import aws.smithy.kotlin.runtime.net.url.Url
import aws.smithy.kotlin.runtime.retries.StandardRetryStrategy
import com.github.cgund98.template.core.config.AppConfig
import com.github.cgund98.template.infrastructure.db.FlywayMigrator
import com.github.cgund98.template.infrastructure.db.TransactionManager
import com.github.cgund98.template.infrastructure.events.publisher.EventPublisher
import com.github.cgund98.template.infrastructure.events.publisher.SnsEventPublisher
import com.github.cgund98.template.infrastructure.events.serializer.EventSerializer
import com.github.cgund98.template.infrastructure.events.serializer.JsonEventSerializer
import com.github.cgund98.template.infrastructure.jooq.JooqTransactionManager
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.jooq.impl.DataSourceConnectionProvider
import org.jooq.impl.DefaultConfiguration
import org.jooq.impl.ThreadLocalTransactionProvider
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import javax.sql.DataSource
import kotlin.time.Duration.Companion.seconds

val localstackCredentials =
    StaticCredentialsProvider {
        accessKeyId = "test"
        secretAccessKey = "test"
    }

/**
 * Shared logic for AWS clients.
 * We use a context parameter (or just two extensions) to keep it DRY.
 */
private fun SqsClient.Config.Builder.applyDefaults() {
    var logger = KotlinLogging.logger { }

    region = AppConfig.data.aws.region
    endpointUrl =
        AppConfig.data.aws.endpoint
            ?.let { Url.parse(it) }

    if (AppConfig.data.aws.useLocalstack) {
        logger.info { "Using localstack for SQS" }
        credentialsProvider = localstackCredentials
    }

    httpClient {
        maxConcurrency = 64u
        connectTimeout = 10.seconds
        socketReadTimeout = 30.seconds
    }
}

// Overload for SNS (They have different concrete types but identical DSLs)
const val SNS_MAX_RETRIES = 3

private fun SnsClient.Config.Builder.applyDefaults() {
    var logger = KotlinLogging.logger { }

    region = AppConfig.data.aws.region
    endpointUrl =
        AppConfig.data.aws.endpoint
            ?.let { Url.parse(it) }

    if (AppConfig.data.aws.useLocalstack) {
        logger.info { "Using localstack for SNS" }
        credentialsProvider = localstackCredentials
    }

    httpClient {
        maxConcurrency = 64u
        connectTimeout = 10.seconds
        socketReadTimeout = 30.seconds
    }

    retryStrategy =
        StandardRetryStrategy {
            maxAttempts = SNS_MAX_RETRIES
        }
}

/**
 * Creates an infrastructure module with the given database configuration.
 * This allows tests to inject their own database configuration (e.g., from testcontainers).
 */
@Suppress("LongMethod")
fun infrastructureModule(
    databaseConfig: DatabaseConfig,
    migrationsPath: String = System.getProperty("user.dir") + "/resources/db/migrations",
): org.koin.core.module.Module =
    module {
        // Database
        single<DataSource> {
            val config =
                HikariConfig().apply {
                    jdbcUrl = databaseConfig.url
                    driverClassName = "org.postgresql.Driver"
                    username = databaseConfig.user
                    password = databaseConfig.password

                    // --- Tuning Parameters ---

                    // The most important setting: How many connections to keep open.
                    // Rule of thumb: (2 * CPU cores) + effective_spindle_count
                    maximumPoolSize = databaseConfig.maxPoolSize

                    // How long to wait for a connection from the pool before throwing an error
                    connectionTimeout = databaseConfig.connectionTimeout // 30 seconds

                    // Helps find "leaked" transactions (connections held too long)
                    leakDetectionThreshold = databaseConfig.leakDetectionThreshold // 2 seconds

                    // Recommended for Postgres performance
                    addDataSourceProperty("cachePrepStmts", "true")
                    addDataSourceProperty("prepStmtCacheSize", "250")
                }

            val dataSource = HikariDataSource(config)

            dataSource
        }

        single<DSLContext> {
            val dataSource: DataSource = get()

            val connectionProvider = DataSourceConnectionProvider(dataSource)
            val transactionProvider = ThreadLocalTransactionProvider(connectionProvider)

            val settings = Settings()

            val configuration =
                DefaultConfiguration()
                    .set(SQLDialect.POSTGRES)
                    .set(connectionProvider)
                    .set(transactionProvider)
                    .set(settings)

            DSL.using(configuration)
        }

        singleOf(::JooqTransactionManager) { bind<TransactionManager>() }

        // Flyway migrator
        single<FlywayMigrator> {
            val dataSource: DataSource = get()
            FlywayMigrator(dataSource, migrationsPath)
        }

        // AWS
        single<SqsClient> {
            runBlocking {
                SqsClient.fromEnvironment { applyDefaults() }
            }
        }

        single<SnsClient> {
            runBlocking {
                SnsClient.fromEnvironment { applyDefaults() }
            }
        }

        // Json
        single<Json> {
            Json {
                ignoreUnknownKeys = true
                prettyPrint = false
                isLenient = true
                encodeDefaults = true
            }
        }

        // Events
        singleOf(::JsonEventSerializer) { bind<EventSerializer>() }

        single<EventPublisher> {
            SnsEventPublisher(
                serializer = get(),
                snsClient = get(),
                topicArn = AppConfig.data.events.topicArn,
            )
        }
    }

/**
 * Convenience infrastructure module that uses AppConfig for production.
 */
val infrastructureModule =
    infrastructureModule(
        databaseConfig =
            DatabaseConfig(
                url = AppConfig.data.postgres.url,
                user = AppConfig.data.postgres.user,
                password = AppConfig.data.postgres.password,
                maxPoolSize = AppConfig.data.postgres.maxPoolSize,
                connectionTimeout = AppConfig.data.postgres.connectionTimeout,
                leakDetectionThreshold = AppConfig.data.postgres.leakDetectionThreshold,
            ),
    )
