package com.github.cgund98.template.domain.user

import com.github.cgund98.template.domain.PageParams
import com.github.cgund98.template.domain.test.setupTestKoin
import com.github.cgund98.template.infrastructure.events.publisher.EventPublisher
import com.github.cgund98.template.infrastructure.events.registry.EventEnvelope
import com.github.cgund98.template.infrastructure.jooq.generated.tables.Users.USERS
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest
import org.jooq.DSLContext
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import java.util.UUID

/**
 * Mock EventPublisher that does nothing - we're testing the service logic, not event publishing.
 */
class NoOpEventPublisher : EventPublisher {
    override suspend fun <T : EventEnvelope<Any>> publish(event: T) {
        // No-op for integration tests
    }
}

class UserServiceIntegrationTest :
    FunSpec(),
    KoinTest {
    private lateinit var stopKoin: () -> Unit

    override suspend fun beforeSpec(spec: io.kotest.core.spec.Spec) {
        // Set up Koin with testcontainer database and override EventPublisher
        stopKoin =
            setupTestKoin(
                additionalModules =
                    listOf(
                        module {
                            single<EventPublisher> { NoOpEventPublisher() }
                        },
                    ),
            )
    }

    override suspend fun afterSpec(spec: io.kotest.core.spec.Spec) {
        stopKoin()
    }

    init {
        beforeEach {
            // Clean database before each test to ensure isolation
            val dsl: DSLContext by inject()
            dsl.deleteFrom(USERS).execute()
        }

        test("createUser should create a user in the database") {
            runTest {
                val userService: UserService by inject()

                val email = "test@example.com"
                val name = "Test User"
                val age = 30

                val user = userService.createUser(email, name, age)

                user.id shouldNotBe null
                user.email shouldBe email
                user.name shouldBe name
                user.age shouldBe age
                user.createdAt shouldNotBe null
                user.updatedAt shouldNotBe null
            }
        }

        test("getUser should retrieve a user by id") {
            runTest {
                val userService: UserService by inject()

                // Create a user first
                val createdUser = userService.createUser("get@example.com", "Get User", 25)

                // Retrieve it
                val retrievedUser = userService.getUser(createdUser.id)

                retrievedUser shouldNotBe null
                retrievedUser?.id shouldBe createdUser.id
                retrievedUser?.email shouldBe "get@example.com"
                retrievedUser?.name shouldBe "Get User"
                retrievedUser?.age shouldBe 25
            }
        }

        test("getUser should return null for non-existent user") {
            runTest {
                val userService: UserService by inject()

                val nonExistentId = UUID.randomUUID()
                val user = userService.getUser(nonExistentId)

                user shouldBe null
            }
        }

        test("updateUser should update an existing user") {
            runTest {
                val userService: UserService by inject()

                // Create a user
                val createdUser = userService.createUser("update@example.com", "Update User", 20)

                // Update the user
                val updatedUser =
                    userService.updateUser(
                        id = createdUser.id,
                        email = "updated@example.com",
                        name = "Updated User",
                        age = 30,
                    )

                updatedUser.id shouldBe createdUser.id
                updatedUser.email shouldBe "updated@example.com"
                updatedUser.name shouldBe "Updated User"
                updatedUser.age shouldBe 30
                updatedUser.updatedAt shouldNotBe createdUser.updatedAt

                // Verify the update persisted
                val retrievedUser = userService.getUser(createdUser.id)
                retrievedUser shouldNotBe null
                retrievedUser?.email shouldBe "updated@example.com"
                retrievedUser?.name shouldBe "Updated User"
                retrievedUser?.age shouldBe 30
            }
        }

        test("updateUser should allow partial updates") {
            runTest {
                val userService: UserService by inject()

                // Create a user
                val createdUser = userService.createUser("partial@example.com", "Partial User", 25)

                // Update only email
                val updatedUser =
                    userService.updateUser(
                        id = createdUser.id,
                        email = "partialupdated@example.com",
                        name = null,
                        age = null,
                    )

                updatedUser.id shouldBe createdUser.id
                updatedUser.email shouldBe "partialupdated@example.com"
                updatedUser.name shouldBe "Partial User" // Unchanged
                updatedUser.age shouldBe 25 // Unchanged
            }
        }

        test("listUsers should return all users with pagination") {
            runTest {
                val userService: UserService by inject()

                // Create multiple users
                val user1 = userService.createUser("list1@example.com", "List User 1", 20)
                val user2 = userService.createUser("list2@example.com", "List User 2", 25)
                val user3 = userService.createUser("list3@example.com", "List User 3", 30)

                // List users (page 1, size 10)
                val page = PageParams(page = 1, size = 10)
                val (users, totalCount) = userService.listUsers(page)

                users.size shouldBe 3
                totalCount shouldBe 3L
                users.map { it.email } shouldBe
                    listOf(
                        user3.email,
                        user2.email,
                        user1.email,
                    ) // Should be ordered by createdAt desc
            }
        }

        test("listUsers should respect pagination") {
            runTest {
                val userService: UserService by inject()

                // Create multiple users
                userService.createUser("page1@example.com", "Page User 1", 20)
                userService.createUser("page2@example.com", "Page User 2", 25)
                userService.createUser("page3@example.com", "Page User 3", 30)

                // Get first page (page 1, size 2)
                val page1 = PageParams(page = 1, size = 2)
                val (users1, totalCount1) = userService.listUsers(page1)

                users1.size shouldBe 2
                totalCount1 shouldBe 3L

                // Get second page (page 2, size 2)
                val page2 = PageParams(page = 2, size = 2)
                val (users2, totalCount2) = userService.listUsers(page2)

                users2.size shouldBe 1
                totalCount2 shouldBe 3L
            }
        }

        test("deleteUser should remove a user from the database") {
            runTest {
                val userService: UserService by inject()

                // Create a user
                val createdUser = userService.createUser("delete@example.com", "Delete User", 30)

                // Verify it exists
                val beforeDelete = userService.getUser(createdUser.id)
                beforeDelete shouldNotBe null

                // Delete it
                userService.deleteUser(createdUser.id)

                // Verify it's gone
                val afterDelete = userService.getUser(createdUser.id)
                afterDelete shouldBe null
            }
        }

        test("full CRUD cycle: create, read, update, delete") {
            runTest {
                val userService: UserService by inject()

                // CREATE
                val createdUser = userService.createUser("crud@example.com", "CRUD User", 25)
                createdUser.id shouldNotBe null

                // READ
                val retrievedUser = userService.getUser(createdUser.id)
                retrievedUser shouldNotBe null
                retrievedUser?.email shouldBe "crud@example.com"

                // UPDATE
                val updatedUser =
                    userService.updateUser(
                        id = createdUser.id,
                        email = "crudupdated@example.com",
                        name = "CRUD Updated",
                        age = 30,
                    )
                updatedUser.email shouldBe "crudupdated@example.com"

                // Verify update persisted
                val afterUpdate = userService.getUser(createdUser.id)
                afterUpdate?.email shouldBe "crudupdated@example.com"

                // DELETE
                userService.deleteUser(createdUser.id)

                // Verify deletion
                val afterDelete = userService.getUser(createdUser.id)
                afterDelete shouldBe null
            }
        }
    }
}
