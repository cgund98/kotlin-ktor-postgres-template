package com.github.cgund98.template.core.config

import io.github.cdimascio.dotenv.dotenv

const val DEV_OPENAPI_PATH = "openapi.json"

object AppConfig {
    private val vars: MutableMap<String, String> = HashMap()

    fun readEnvFiles() {
        // Load .env and .env.local
        listOf(".env", ".env.local").forEach { filename ->
            val env =
                dotenv {
                    this.filename = filename
                    ignoreIfMissing = true

                    //
                    // Make sure you have this defined in build.gradle.kts for the entrypoint module
                    //
                    // tasks.withType<JavaExec> {
                    //     workingDir = rootProject.projectDir
                    // }
                    //
                    directory = "./"
                }

            env.entries().forEach {
                vars[it.key] = it.value
                System.setProperty(it.key, it.value) // Do this so logback can detect values
            }
        }
    }

    init {
        readEnvFiles()
    }

    val data: Settings =
        Settings(
            postgres =
                PostgresSettings(
                    url = vars["POSTGRES_URL"] ?: "",
                    user = vars["POSTGRES_USER"] ?: "",
                    password = vars["POSTGRES_PASSWORD"] ?: "",
                    maxPoolSize = vars["POSTGRES_MAX_POOL_SIZE"]?.toInt() ?: 10,
                    connectionTimeout = vars["POSTGRES_CONNECTION_TIMEOUT"]?.toLong() ?: 25000,
                    leakDetectionThreshold =
                        vars["POSTGRES_LEAK_DETECTION_THRESHOLD"]?.toLong() ?: 2000,
                ),
            api =
                ApiSettings(
                    port = vars["API_PORT"]?.toInt() ?: 8000,
                    openApiPath = vars["API_OPENAPI_PATH"] ?: DEV_OPENAPI_PATH,
                ),
            aws =
                AwsSettings(
                    region = vars["AWS_REGION"] ?: "",
                    endpoint = vars["AWS_ENDPOINT"],
                    useLocalstack = vars["AWS_USE_LOCALSTACK"]?.toBoolean() ?: false,
                ),
            events =
                EventsSettings(
                    topicArn = vars["EVENTS_TOPIC_ARN"] ?: "",
                    queueUrlUserCreated = vars["EVENTS_QUEUE_URL_USER_CREATED"] ?: "",
                    queueUrlUserUpdated = vars["EVENTS_QUEUE_URL_USER_UPDATED"] ?: "",
                    queueUrlUserDeleted = vars["EVENTS_QUEUE_URL_USER_DELETED"] ?: "",
                ),
        )
}

data class Settings(
    val postgres: PostgresSettings,
    val api: ApiSettings,
    val aws: AwsSettings,
    val events: EventsSettings,
)

data class PostgresSettings(
    val url: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int,
    val connectionTimeout: Long,
    val leakDetectionThreshold: Long,
)

data class ApiSettings(
    val port: Int,
    val openApiPath: String,
)

data class AwsSettings(
    val region: String,
    val endpoint: String?,
    val useLocalstack: Boolean,
)

data class EventsSettings(
    val topicArn: String,
    val queueUrlUserCreated: String,
    val queueUrlUserUpdated: String,
    val queueUrlUserDeleted: String,
)
