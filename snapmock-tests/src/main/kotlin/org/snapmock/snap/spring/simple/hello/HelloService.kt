package org.snapmock.snap.spring.simple.hello

import org.snapmock.core.Snap
import org.springframework.stereotype.Service

@Snap
@Service
class HelloService(
    private val helloRepository: HelloRepository
) {

    fun get(): HelloData {
        return HelloData(helloRepository.message)
    }

    fun post(data: String): HelloData {
        return helloRepository.post(data)
    }
}
