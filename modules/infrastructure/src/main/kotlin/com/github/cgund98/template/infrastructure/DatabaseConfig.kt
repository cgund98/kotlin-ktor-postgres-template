package com.github.cgund98.template.infrastructure

/**
 * Configuration for database connection settings.
 * Used to parameterize the infrastructure module for testing.
 */
data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int = 10,
    val connectionTimeout: Long = 25000,
    val leakDetectionThreshold: Long = 2000,
)
