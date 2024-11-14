package org.snapmock.snap.spring.data.author

import org.springframework.data.jpa.repository.JpaRepository

interface AuthorRepository: JpaRepository<AuthorEntity, Int> {}
