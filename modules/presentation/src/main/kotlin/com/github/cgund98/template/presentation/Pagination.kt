package com.github.cgund98.template.presentation

import com.github.cgund98.template.domain.PageParams
import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.Serializable
import kotlin.text.toIntOrNull

const val MIN_SIZE = 1
const val MAX_SIZE = 100
const val DEFAULT_SIZE = 25

data class PaginationParams(
    val page: Int,
    val size: Int,
) {
    fun toDomain(): PageParams = PageParams(page, size)
}

@Serializable
data class PaginationMetadata(
    val page: Int,
    val size: Int,
    val totalPages: Int,
    val totalItems: Long,
    val last: Boolean = page >= totalPages,
) {
    companion object {
        fun from(
            params: PaginationParams,
            totalItems: Long,
        ): PaginationMetadata {
            val page = params.page
            val size = params.size

            val totalPages = (totalItems + size - 1) / size
            return PaginationMetadata(
                page = page,
                size = size,
                totalPages = totalPages.toInt(),
                totalItems = totalItems,
            )
        }
    }
}

fun RoutingContext.parsePaginationParams(): PaginationParams {
    val page = call.parameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
    val size =
        call.parameters["size"]
            ?.toIntOrNull()
            ?.coerceAtLeast(MIN_SIZE)
            ?.coerceAtMost(MAX_SIZE) ?: DEFAULT_SIZE

    return PaginationParams(page, size)
}
