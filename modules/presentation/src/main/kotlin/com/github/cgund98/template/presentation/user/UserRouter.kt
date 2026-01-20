package com.github.cgund98.template.presentation.user

import com.github.cgund98.template.domain.user.UserService
import com.github.cgund98.template.presentation.MissingParameterException
import com.github.cgund98.template.presentation.PaginationMetadata
import com.github.cgund98.template.presentation.parsePaginationParams
import com.github.cgund98.template.presentation.parseUUIDParameter
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.patch
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.route
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import org.koin.ktor.ext.inject
import java.util.UUID

fun RoutingContext.getIdParameter(): UUID {
    val rawId = call.parameters["id"] ?: throw MissingParameterException("id")
    return parseUUIDParameter("id", rawId)
}

fun Route.userRoutes() {
    val userService by inject<UserService>()

    route("/users", {
        tags = listOf("User")
    }) {
        get({
            description = "List all users"
            request {
                queryParameter<Int>("page") {
                    description = "Page number"
                }
                queryParameter<Int>("size") {
                    description = "Number of items per page"
                }
            }
            response {
                HttpStatusCode.OK to {
                    description = "Successful retrieval"
                    body<ListUsersResponse>()
                }
            }
        }) {
            val pageParams = parsePaginationParams()
            val (users, count) = userService.listUsers(page = pageParams.toDomain())

            val response =
                ListUsersResponse(
                    users = users.map { it.toResponse() },
                    pagination =
                        PaginationMetadata.from(params = pageParams, totalItems = count),
                )
            call.respond(response)
        }

        get("/{id}", {
            description = "Get user by ID"
            request {
                pathParameter<String>("id") {
                    description = "User ID"
                }
            }
            response {
                HttpStatusCode.OK to {
                    description = "Successful retrieval"
                    body<GetUserResponse>()
                }
            }
        }) {
            val id = getIdParameter()

            val user = userService.getUser(id)
            val response = GetUserResponse(user = user?.toResponse())
            call.respond(response)
        }

        post({
            description = "Create a new user"
            request {
                body<UserCreateRequest>()
            }
            response {
                HttpStatusCode.Created to {
                    description = "User created successfully"
                    body<UserResponse>()
                }
            }
        }) {
            val request = call.receive<UserCreateRequest>()
            val created = userService.createUser(email = request.email, name = request.name, age = request.age)
            call.respond(HttpStatusCode.Created, created.toResponse())
        }

        patch("/{id}", {
            description = "Update user by ID"
            request {
                pathParameter<String>("id") {
                    description = "User ID"
                }
                body<UserUpdateRequest>()
            }
            response {
                HttpStatusCode.OK to {
                    description = "User updated successfully"
                    body<UserResponse>()
                }
            }
        }) {
            val id = getIdParameter()

            val request = call.receive<UserUpdateRequest>()
            val user =
                userService.updateUser(
                    id = id,
                    email = request.email,
                    name = request.name,
                    age = request.age,
                )

            call.respond(HttpStatusCode.OK, user.toResponse())
        }

        delete("/{id}", {
            description = "Delete user by ID"
            request {
                pathParameter<String>("id") {
                    description = "User ID"
                }
            }
            response {
                HttpStatusCode.OK to {
                    description = "User deleted successfully"
                    body<DeleteUserResponse>()
                }
            }
        }) {
            val id = getIdParameter()

            userService.deleteUser(id)
            val response = DeleteUserResponse(success = true)
            call.respond(HttpStatusCode.OK, response)
        }
    }
}
