package org.snapmock.snap.spring.data.book

import org.snapmock.core.Snap
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Snap
@Service
class BookService(
    private val bookRepository: BookRepository
) {

    @Transactional(readOnly = true)
    fun getBooks(): List<BookEntity> {
        return bookRepository.findAll()
    }

    @Transactional(readOnly = true)
    fun getBooksArray(): Array<BookEntity> {
        return bookRepository.findAllAsArray()
    }

}
