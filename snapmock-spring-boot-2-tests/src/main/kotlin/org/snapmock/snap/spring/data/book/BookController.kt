package org.snapmock.snap.spring.data.book

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController(value = "/books")
class BookController(
    private val bookService: BookService
) {

    @GetMapping
    fun getBooks(): List<BookDto> {
        return bookService.getBooks().map { it.toDto() }
    }

}
