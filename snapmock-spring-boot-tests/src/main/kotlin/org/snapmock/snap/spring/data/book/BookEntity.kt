package org.snapmock.snap.spring.data.book

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToMany
import org.snapmock.snap.spring.data.author.AuthorEntity

@Entity
data class BookEntity(
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int? = null,
    var title: String,
    @ManyToMany(fetch = FetchType.LAZY, targetEntity = AuthorEntity::class)
    var authors: List<AuthorEntity>,
)
