package io.github.sickfar.snapmock.spring.data.author

import io.github.sickfar.snapmock.core.Snap
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@Service
class AuthorService(
    private val authorRepository: AuthorRepository,
    private val platformTransactionManager: PlatformTransactionManager
) {

    @Snap
    fun create(author: AuthorEntity): AuthorEntity {
        val transactionTemplate = TransactionTemplate(platformTransactionManager)
        return transactionTemplate.execute { _ ->
            authorRepository.save(author)
        }
    }

}
