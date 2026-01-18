package com.github.cgund98.template.domain.user.repo

import com.github.cgund98.template.infrastructure.db.NotFoundException
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

object ColumnLengths {
    const val EMAIL = 255
    const val NAME = 100
}

object UsersTable : UUIDTable("users") {
    // The 'id' column is automatically created by UUIDTable

    val email = varchar("email", ColumnLengths.EMAIL)
    val name = varchar("name", ColumnLengths.NAME)
    val age = integer(name = "age").nullable()

    // Audit timestamps
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

private fun ResultRow.toUserEntity() =
    UserEntity(
        id = this[UsersTable.id].value,
        email = this[UsersTable.email],
        name = this[UsersTable.name],
        age = this[UsersTable.age],
        createdAt = this[UsersTable.createdAt],
        updatedAt = this[UsersTable.updatedAt],
    )

class ExposedUserRepository : UserRepository {
    override suspend fun create(params: CreateUserParams): UserEntity {
        val userId =
            UsersTable.insert {
                it[email] = params.email
                it[name] = params.name
                it[age] = params.age
            } get UsersTable.id

        return findById(userId.value)
            ?: throw NotFoundException("User", userId.toString())
    }

    override suspend fun findById(id: UUID): UserEntity? =
        UsersTable
            .selectAll()
            .where(UsersTable.id eq id)
            .firstOrNull()
            ?.toUserEntity()

    override suspend fun findByEmail(email: String): UserEntity? =
        UsersTable
            .selectAll()
            .where(UsersTable.email eq email)
            .firstOrNull()
            ?.toUserEntity()

    @OptIn(ExperimentalTime::class)
    override suspend fun update(params: UpdateUserParams): UserEntity {
        UsersTable.update({ UsersTable.id eq params.id }) {
            params.email?.let { newEmail -> it[email] = newEmail }
            params.name?.let { newName -> it[name] = newName }
            params.age?.let { newAge -> it[age] = newAge }
            it[updatedAt] = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        }

        return findById(params.id)
            ?: throw NotFoundException("User", params.id.toString())
    }

    override suspend fun delete(id: UUID): Boolean {
        UsersTable.deleteWhere { UsersTable.id eq id }
        return findById(id) == null
    }

    override suspend fun count(filter: UserFilter): Long {
        val query =
            UsersTable.select(UsersTable.id.count())

        filter.email?.let { email -> query.andWhere { UsersTable.email eq email } }

        return query.first()[UsersTable.id.count()]
    }

    override suspend fun list(
        limit: Int,
        offset: Long,
        filter: UserFilter,
    ): List<UserEntity> {
        val query =
            UsersTable.selectAll().limit(limit).offset(offset)

        filter.email?.let { email -> query.andWhere { UsersTable.email eq email } }

        return query.map { it.toUserEntity() }
    }
}
