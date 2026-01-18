package com.github.cgund98.template.infrastructure.exposed

import com.github.cgund98.template.infrastructure.db.TransactionManager
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

class ExposedTransactionManager(
    private val database: Database,
) : TransactionManager {
    override suspend fun <T> withTransaction(block: suspend () -> T): T =
        suspendTransaction(db = database) {
            block()
        }
}
