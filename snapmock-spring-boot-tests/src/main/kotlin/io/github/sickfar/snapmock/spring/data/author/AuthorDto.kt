package io.github.sickfar.snapmock.spring.data.author

data class AuthorDto(
    val id: Int?,
    val name: String
)

fun AuthorEntity.toDto(): AuthorDto = AuthorDto(
    id = this.id,
    name = this.name
)

fun AuthorDto.toNew(): AuthorEntity = AuthorEntity(
    id = null,
    name = this.name
)
