package com.github.cgund98.template.presentation.user

import com.github.cgund98.template.domain.user.UserService
import com.github.cgund98.template.presentation.PaginationMetadata
import com.github.cgund98.template.presentation.parsePaginationParams
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.userRoutes() {
    val userService by inject<UserService>()

    /*
     * @tag User
     */
    route("/users") {
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

        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val user = userService.getUser(UUID.fromString(id))
            val response = user?.let { GetUserResponse(user = it.toResponse()) } ?: GetUserResponse(user = null)
            call.respond(response)
        }

        post {
            val request = call.receive<UserCreateRequest>()
            val created = userService.createUser(email = request.email, name = request.name, age = request.age)
            call.respond(HttpStatusCode.Created, created.toResponse())
        }
    }
}
