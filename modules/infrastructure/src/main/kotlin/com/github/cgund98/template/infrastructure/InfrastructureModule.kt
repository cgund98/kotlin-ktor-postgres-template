package com.github.cgund98.template.infrastructure

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.sns.SnsClient
import aws.sdk.kotlin.services.sqs.SqsClient
import aws.smithy.kotlin.runtime.net.url.Url
import aws.smithy.kotlin.runtime.retries.StandardRetryStrategy
import com.github.cgund98.template.core.config.AppConfig
import com.github.cgund98.template.infrastructure.db.TransactionManager
import com.github.cgund98.template.infrastructure.events.publisher.EventPublisher
import com.github.cgund98.template.infrastructure.events.publisher.SnsEventPublisher
import com.github.cgund98.template.infrastructure.events.serializer.EventSerializer
import com.github.cgund98.template.infrastructure.events.serializer.JsonEventSerializer
import com.github.cgund98.template.infrastructure.exposed.ExposedTransactionManager
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
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
    region = AppConfig.data.aws.region
    endpointUrl =
        AppConfig.data.aws.endpoint
            ?.let { Url.parse(it) }

    if (AppConfig.data.aws.useLocalstack) {
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
    region = AppConfig.data.aws.region
    endpointUrl =
        AppConfig.data.aws.endpoint
            ?.let { Url.parse(it) }

    if (AppConfig.data.aws.useLocalstack) {
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

val infrastructureModule =
    module {
        // Database
        single<Database> {
            val config =
                HikariConfig().apply {
                    jdbcUrl = AppConfig.data.postgres.url
                    driverClassName = "org.postgresql.Driver"
                    username = AppConfig.data.postgres.user
                    password = AppConfig.data.postgres.password

                    // --- Tuning Parameters ---

                    // The most important setting: How many connections to keep open.
                    // Rule of thumb: (2 * CPU cores) + effective_spindle_count
                    maximumPoolSize = AppConfig.data.postgres.maxPoolSize

                    // How long to wait for a connection from the pool before throwing an error
                    connectionTimeout = AppConfig.data.postgres.connectionTimeout // 30 seconds

                    // Helps find "leaked" transactions (connections held too long)
                    leakDetectionThreshold = AppConfig.data.postgres.leakDetectionThreshold // 2 seconds

                    // Recommended for Postgres performance
                    addDataSourceProperty("cachePrepStmts", "true")
                    addDataSourceProperty("prepStmtCacheSize", "250")
                }

            val dataSource = HikariDataSource(config)

            // Pass the dataSource object instead of the URL string
            Database.connect(dataSource)
        }

        singleOf(::ExposedTransactionManager) { bind<TransactionManager>() }

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
