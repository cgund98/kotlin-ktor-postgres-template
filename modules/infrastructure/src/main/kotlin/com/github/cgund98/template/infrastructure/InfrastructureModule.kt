package com.github.cgund98.template.infrastructure

import com.github.cgund98.template.core.config.AppConfig
import com.github.cgund98.template.infrastructure.db.TransactionManager
import com.github.cgund98.template.infrastructure.exposed.ExposedTransactionManager
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val infrastructureModule =
    module {
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
    }
