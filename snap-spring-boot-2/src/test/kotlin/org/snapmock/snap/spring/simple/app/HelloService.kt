package org.snapmock.snap.spring.simple.app

import org.springframework.stereotype.Service

@Service
open class HelloService {

    open fun get(): HelloData {
        return HelloData("Hello world")
    }
}
