package org.snapmock.snap.spring.data.book

import org.snapmock.snap.spring.data.author.AuthorDto
import org.snapmock.snap.spring.data.author.toDto

data class BookDto(
    val id: Int?,
    val title: String,
    val author: List<AuthorDto>,
)

data class BookDtoShort(
    val id: Int?,
    val title: String,
)

fun BookEntity.toDto(): BookDto = BookDto(
    id = id,
    title = title,
    author = authors.map { it.toDto() },
)
