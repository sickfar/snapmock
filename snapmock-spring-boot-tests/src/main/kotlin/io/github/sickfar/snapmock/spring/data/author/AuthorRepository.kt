package io.github.sickfar.snapmock.spring.data.author

import org.springframework.data.jpa.repository.JpaRepository

interface AuthorRepository: JpaRepository<AuthorEntity, Int>
