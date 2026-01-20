package com.github.cgund98.template.presentation.user

import com.github.cgund98.template.domain.user.UserService
import com.github.cgund98.template.presentation.MissingParameterException
import com.github.cgund98.template.presentation.PaginationMetadata
import com.github.cgund98.template.presentation.parsePaginationParams
import com.github.cgund98.template.presentation.parseUUIDParameter
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject
import java.util.UUID

fun RoutingContext.getIdParameter(): UUID {
    val rawId = call.parameters["id"] ?: throw MissingParameterException("id")
    return parseUUIDParameter("id", rawId)
}

fun Route.userRoutes() {
    val userService by inject<UserService>()

    /**
     * @tag User
     */
    route("/users") {
        /**
         * @query page [Int] Page number
         * @query size [Int] Number of items per page
         */
        get {
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

        /**
         * @path id [String] User ID
         */
        get("/{id}") {
            val id = getIdParameter()

            val user = userService.getUser(id)
            val response = GetUserResponse(user = user?.toResponse())
            call.respond(response)
        }

        post {
            val request = call.receive<UserCreateRequest>()
            val created = userService.createUser(email = request.email, name = request.name, age = request.age)
            call.respond(HttpStatusCode.Created, created.toResponse())
        }

        /**
         * @path id [String] User ID
         */
        patch("/{id}") {
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

        /**
         * @path id [String] User ID
         */
        delete("/{id}") {
            val id = getIdParameter()

            userService.deleteUser(id)
            val response = DeleteUserResponse(success = true)
            call.respond(HttpStatusCode.OK, response)
        }
    }
}
