package io.github.sickfar.snapmock.spring.data.book

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface BookRepository: JpaRepository<BookEntity, Int> {

    @Query("select e from BookEntity e")
    fun findAllAsArray(): Array<BookEntity>

}
