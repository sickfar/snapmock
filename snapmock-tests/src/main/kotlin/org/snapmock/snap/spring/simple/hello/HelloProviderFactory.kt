package org.snapmock.snap.spring.simple.hello

import org.snapmock.core.SnapDepFactory
import org.springframework.stereotype.Component

enum class HelloFactory {
    CLASS, METHOD, FIELD, SETTER
}

@Component
@SnapDepFactory
class HelloProviderFactoryByClass {

    val provider: HelloDataProvider
        get() = HelloDataProvider("Hello from ${HelloFactory.CLASS}")

}

@Component
class HelloProviderFactoryByMethod {

    @get:SnapDepFactory
    val provider: HelloDataProvider
        get() = HelloDataProvider("Hello from ${HelloFactory.METHOD}")

}

@Component
class HelloProviderFactoryByField {

    val provider: HelloDataProvider
        get() = HelloDataProvider("Hello from ${HelloFactory.FIELD}")

}

@Component
class HelloProviderFactoryBySetter {

    val provider: HelloDataProvider
        get() = HelloDataProvider("Hello from ${HelloFactory.SETTER}")

}
