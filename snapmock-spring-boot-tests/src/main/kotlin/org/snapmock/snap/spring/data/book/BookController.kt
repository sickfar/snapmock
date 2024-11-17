package org.snapmock.snap.spring.data.book

import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class BookController(
    private val bookService: BookService
) {

    @GetMapping(value = ["/books"], produces = [APPLICATION_JSON_VALUE])
    fun getBooks(): List<BookDto> {
        return bookService.getBooks().map { it.toDto() }
    }

    @GetMapping(value = ["/books/array"], produces = [APPLICATION_JSON_VALUE])
    fun getBooksArray(): List<BookDto> {
        return bookService.getBooksArray().map { it.toDto() }
    }

}
