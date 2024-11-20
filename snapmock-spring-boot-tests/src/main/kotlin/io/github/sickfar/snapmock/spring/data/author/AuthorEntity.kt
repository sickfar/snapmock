package io.github.sickfar.snapmock.spring.data.author

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
data class AuthorEntity(
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int? = null,
    var name: String,
)
