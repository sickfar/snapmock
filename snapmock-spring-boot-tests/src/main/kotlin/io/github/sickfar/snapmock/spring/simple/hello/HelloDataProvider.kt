package io.github.sickfar.snapmock.spring.simple.hello

open class HelloDataProvider(
    private val greeting: String
) {

    open fun getGreeting(): String {
        return greeting
    }
}
