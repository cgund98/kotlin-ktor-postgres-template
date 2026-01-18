package com.github.cgund98.template.infrastructure

import com.github.cgund98.template.core.config.AppConfig
import com.github.cgund98.template.infrastructure.db.TransactionManager
import com.github.cgund98.template.infrastructure.exposed.ExposedTransactionManager
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val infrastructureModule =
    module {
        single<Database> {
            Database.connect(
                url = AppConfig.data.postgres.url,
                driver = "org.postgresql.Driver",
                user = AppConfig.data.postgres.user,
                password = AppConfig.data.postgres.password,
            )
        }

        singleOf(::ExposedTransactionManager) { bind<TransactionManager>() }
    }
