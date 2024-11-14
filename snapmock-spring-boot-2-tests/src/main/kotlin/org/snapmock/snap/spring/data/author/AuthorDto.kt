package org.snapmock.snap.spring.data.author

data class AuthorDto(
    val id: Int?,
    val name: String
)

fun AuthorEntity.toDto(): AuthorDto = AuthorDto(
    id = this.id,
    name = this.name
)
