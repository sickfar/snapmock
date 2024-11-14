package org.snapmock.snap.spring.data.author

import org.springframework.stereotype.Service

@Service
class AuthorService(
    private val authorRepository: AuthorRepository
) {
}
