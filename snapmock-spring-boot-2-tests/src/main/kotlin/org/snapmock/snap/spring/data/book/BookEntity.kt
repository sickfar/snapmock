package org.snapmock.snap.spring.data.book

import org.snapmock.snap.spring.data.author.AuthorEntity
import javax.persistence.*

@Entity
class BookEntity(
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int? = null,
    var title: String,
    @ManyToOne(fetch = FetchType.LAZY)
    var author: AuthorEntity,
)
