package org.snapmock.snap.spring.data.book

import org.snapmock.snap.spring.data.author.AuthorEntity
import javax.persistence.*

@Entity
data class BookEntity(
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int? = null,
    var title: String,
    @ManyToMany(fetch = FetchType.LAZY)
    var authors: List<AuthorEntity>,
)
