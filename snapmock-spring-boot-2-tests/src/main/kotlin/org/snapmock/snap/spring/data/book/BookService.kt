package org.snapmock.snap.spring.data.book

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BookService(
    private val bookRepository: BookRepository
) {

    @Transactional(readOnly = true)
    fun getBooks(): List<BookEntity> = bookRepository.findAll()

}
