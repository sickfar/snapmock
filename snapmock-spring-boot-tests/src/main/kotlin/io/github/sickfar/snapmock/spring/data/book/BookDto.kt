package io.github.sickfar.snapmock.spring.data.book

import io.github.sickfar.snapmock.spring.data.author.AuthorDto
import io.github.sickfar.snapmock.spring.data.author.toDto

data class BookDto(
    val id: Int?,
    val title: String,
    val author: List<AuthorDto>,
)

fun BookEntity.toDto(): BookDto = BookDto(
    id = id,
    title = title,
    author = authors.map { it.toDto() },
)
