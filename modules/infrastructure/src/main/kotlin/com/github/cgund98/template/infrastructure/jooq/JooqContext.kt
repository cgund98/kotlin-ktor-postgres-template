package com.github.cgund98.template.infrastructure.jooq

import org.jooq.DSLContext
import kotlin.coroutines.CoroutineContext

/**
 * Coroutine context key for accessing the transaction-scoped DSLContext.
 * When inside a transaction, this provides the transaction-specific DSLContext.
 */
object JooqContextKey : CoroutineContext.Key<JooqContextElement>

/**
 * Coroutine context element that holds the transaction-scoped DSLContext.
 */
data class JooqContextElement(
    val dsl: DSLContext,
) : CoroutineContext.Element {
    override val key: CoroutineContext.Key<*> = JooqContextKey
}

/**
 * Extension function to get the DSLContext from the current coroutine context.
 * Throws an error if not in a transaction context.
 */
fun CoroutineContext.jooq(): DSLContext? = get(JooqContextKey)?.dsl
