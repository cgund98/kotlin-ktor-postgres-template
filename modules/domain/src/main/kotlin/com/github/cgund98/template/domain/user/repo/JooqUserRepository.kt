package com.github.cgund98.template.domain.user.repo

import com.github.cgund98.template.infrastructure.db.NotFoundException
import com.github.cgund98.template.infrastructure.jooq.generated.tables.Users.USERS
import com.github.cgund98.template.infrastructure.jooq.generated.tables.records.UsersRecord
import com.github.cgund98.template.infrastructure.jooq.jooq
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.datetime.toKotlinLocalDateTime
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.impl.DSL
import java.util.UUID

private fun UsersRecord.toUserEntity(): UserEntity =
    UserEntity(
        id = id,
        email = email,
        name = name,
        age = age,
        createdAt = createdAt.toKotlinLocalDateTime(),
        updatedAt = updatedAt.toKotlinLocalDateTime(),
    )

class JooqUserRepository(
    private val dsl: DSLContext,
) : UserRepository {
    /**
     * Gets the DSLContext from the coroutine context (if in a transaction)
     * or falls back to the injected DSLContext.
     */
    private suspend fun getDsl(): DSLContext = currentCoroutineContext().jooq() ?: dsl

    private fun where(filter: UserFilter): Condition = filter.email?.let { USERS.EMAIL.eq(it) } ?: DSL.trueCondition()

    override suspend fun create(params: CreateUserParams): UserEntity {
        val id = UUID.randomUUID()
        val transactionDsl = getDsl()

        val record: UsersRecord =
            transactionDsl
                .insertInto(USERS)
                .set(USERS.ID, id)
                .set(USERS.EMAIL, params.email)
                .set(USERS.NAME, params.name)
                .set(USERS.AGE, params.age)
                .returning()
                .fetchOne()
                ?: throw NotFoundException("User", id.toString())

        return record.toUserEntity()
    }

    override suspend fun findById(id: UUID): UserEntity? {
        val transactionDsl = getDsl()
        return transactionDsl
            .selectFrom(USERS)
            .where(USERS.ID.eq(id))
            .fetchOne()
            ?.toUserEntity()
    }

    override suspend fun findByEmail(email: String): UserEntity? {
        val transactionDsl = getDsl()
        return transactionDsl
            .selectFrom(USERS)
            .where(USERS.EMAIL.eq(email))
            .fetchOne()
            ?.toUserEntity()
    }

    override suspend fun update(params: UpdateUserParams): UserEntity {
        val transactionDsl = getDsl()
        val update =
            transactionDsl
                .update(USERS)
                .set(USERS.UPDATED_AT, DSL.currentLocalDateTime())

        params.email?.let { update.set(USERS.EMAIL, it) }
        params.name?.let { update.set(USERS.NAME, it) }
        params.age?.let { update.set(USERS.AGE, it) }

        val record: UsersRecord =
            update
                .where(USERS.ID.eq(params.id))
                .returning()
                .fetchOne()
                ?: throw NotFoundException("User", params.id.toString())

        return record.toUserEntity()
    }

    override suspend fun delete(id: UUID): Boolean {
        val transactionDsl = getDsl()
        transactionDsl
            .deleteFrom(USERS)
            .where(USERS.ID.eq(id))
            .execute()

        return findById(id) == null
    }

    override suspend fun count(filter: UserFilter): Long {
        val transactionDsl = getDsl()
        return transactionDsl.fetchCount(USERS, where(filter)).toLong()
    }

    override suspend fun list(
        limit: Int,
        offset: Long,
        filter: UserFilter,
    ): List<UserEntity> {
        val transactionDsl = getDsl()
        return transactionDsl
            .selectFrom(USERS)
            .where(where(filter))
            .orderBy(USERS.CREATED_AT.desc())
            .limit(limit)
            .offset(offset.toInt())
            .fetch()
            .map { it.toUserEntity() }
    }
}
