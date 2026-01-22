package com.github.cgund98.template.domain.user.repo

import com.github.cgund98.template.infrastructure.db.NotFoundException
import com.github.cgund98.template.infrastructure.jooq.JooqRepository
import com.github.cgund98.template.infrastructure.jooq.generated.tables.Users.USERS
import com.github.cgund98.template.infrastructure.jooq.generated.tables.records.UsersRecord
import com.github.cgund98.template.infrastructure.jooq.withDsl
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
    override val dsl: DSLContext,
) : UserRepository,
    JooqRepository {
    /**
     * Gets the DSLContext from the coroutine context (if in a transaction)
     * or falls back to the injected DSLContext.
     *
     * Wraps the queries in a Dispatchers.IO context as these will be blocking calls.
     */

    private fun whereFromFilters(filter: UserFilter): Condition =
        filter.email?.let { USERS.EMAIL.eq(it) } ?: DSL.noCondition()

    override suspend fun create(params: CreateUserParams): UserEntity =
        withDsl {
            val id = UUID.randomUUID()

            val record: UsersRecord =
                insertInto(USERS)
                    .set(USERS.ID, id)
                    .set(USERS.EMAIL, params.email)
                    .set(USERS.NAME, params.name)
                    .set(USERS.AGE, params.age)
                    .returning()
                    .fetchOne()
                    ?: throw NotFoundException("User", id.toString())

            record.toUserEntity()
        }

    override suspend fun findById(id: UUID): UserEntity? =
        withDsl {
            selectFrom(USERS)
                .where(USERS.ID.eq(id))
                .fetchOne()
                ?.toUserEntity()
        }

    override suspend fun findByEmail(email: String): UserEntity? =
        withDsl {
            selectFrom(USERS)
                .where(USERS.EMAIL.eq(email))
                .fetchOne()
                ?.toUserEntity()
        }

    override suspend fun update(params: UpdateUserParams): UserEntity =
        withDsl {
            update(USERS)
                .set(USERS.UPDATED_AT, DSL.currentLocalDateTime())
                .apply {
                    params.email?.let { set(USERS.EMAIL, it) }
                    params.name?.let { set(USERS.NAME, it) }
                    params.age?.let { set(USERS.AGE, it) }
                }.where(USERS.ID.eq(params.id))
                .returning()
                .fetchOne()
                ?.toUserEntity()
                ?: throw NotFoundException("User", params.id.toString())
        }

    override suspend fun delete(id: UUID): Boolean =
        withDsl {
            deleteFrom(USERS)
                .where(USERS.ID.eq(id))
                .execute()

            findById(id) == null
        }

    override suspend fun count(filter: UserFilter): Long =
        withDsl {
            fetchCount(USERS, whereFromFilters(filter)).toLong()
        }

    override suspend fun list(
        limit: Int,
        offset: Long,
        filter: UserFilter,
    ): List<UserEntity> =
        withDsl {
            selectFrom(USERS)
                .where(whereFromFilters(filter))
                .orderBy(USERS.CREATED_AT.desc())
                .limit(limit)
                .offset(offset.toInt())
                .fetch()
                .map { it.toUserEntity() }
        }
}
