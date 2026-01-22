package com.github.cgund98.template.infrastructure.jooq

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import org.jooq.DSLContext

interface JooqRepository {
    val dsl: DSLContext
}

suspend fun <T> JooqRepository.withDsl(block: suspend DSLContext.() -> T): T =
    withContext(Dispatchers.IO) {
        val activeDsl = currentCoroutineContext().jooq() ?: dsl
        activeDsl.block()
    }
