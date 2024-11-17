package io.github.sickfar.snapmock.spring.simple.hello

import org.springframework.stereotype.Service

@Service
class HelloRepository {

    val message: String = "Hello World"

    fun post(data: String): HelloData {
        return HelloData(data)
    }

    fun getNull(nothing: Nothing?): HelloData? = null

}
