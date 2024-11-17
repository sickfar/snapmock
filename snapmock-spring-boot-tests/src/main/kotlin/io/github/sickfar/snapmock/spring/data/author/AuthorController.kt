package io.github.sickfar.snapmock.spring.data.author

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthorController(
    private val authorService: AuthorService
) {

    @PostMapping("/authors")
    fun createAuthor(@RequestBody author: AuthorDto): AuthorDto {
        return authorService.create(author.toNew()).toDto()
    }

}
