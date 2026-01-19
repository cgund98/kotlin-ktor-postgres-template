package com.github.cgund98.template.infrastructure.exposed

import com.github.cgund98.template.infrastructure.db.TransactionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

class ExposedTransactionManager(
    private val database: Database,
) : TransactionManager {
    override suspend fun <T> withTransaction(block: suspend () -> T): T =
        // Transaction should only contain DB-related IO operations
        // This dispatcher is meant for tasks where the CPU is idle and waiting for a response.
        // Do not execute HTTP requests or similar tasks here.
        withContext(Dispatchers.IO) {
            suspendTransaction(db = database) {
                block()
            }
        }
}
