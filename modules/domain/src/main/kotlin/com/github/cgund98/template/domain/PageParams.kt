package com.github.cgund98.template.domain

data class PageParams(
    val page: Int = 1,
    val size: Int = 20,
) {
    val offset: Long = (page.toLong() - 1) * size.toLong()
}
