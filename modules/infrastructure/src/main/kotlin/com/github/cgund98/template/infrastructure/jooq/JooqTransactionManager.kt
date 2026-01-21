package com.github.cgund98.template.infrastructure.jooq

import com.github.cgund98.template.infrastructure.db.TransactionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jooq.DSLContext

class JooqTransactionManager(
    private val dsl: DSLContext,
) : TransactionManager {
    override suspend fun <T> withTransaction(block: suspend () -> T): T =
        // Transaction should only contain DB-related IO operations
        // This dispatcher is meant for tasks where the CPU is idle and waiting for a response.
        // Do not execute HTTP requests or similar tasks here.
        withContext(Dispatchers.IO) {
            dsl.transactionResult { txConfig ->
                runBlocking {
                    // Provide the transaction-scoped DSLContext in the coroutine context
                    withContext(JooqContextElement(txConfig.dsl())) {
                        block()
                    }
                }
            }
        }
}
