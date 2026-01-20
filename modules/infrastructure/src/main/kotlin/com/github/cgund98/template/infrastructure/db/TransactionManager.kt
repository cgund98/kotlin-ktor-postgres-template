package com.github.cgund98.template.infrastructure.db

interface TransactionManager {
    suspend fun <T> withTransaction(block: suspend () -> T): T
}
