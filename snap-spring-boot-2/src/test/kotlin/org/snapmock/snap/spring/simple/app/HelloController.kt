package org.snapmock.snap.spring.simple.app

import org.snapmock.core.Snap
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@Snap
@RestController(value = "/hello")
open class HelloController(
    private val service: HelloService
) {

    @GetMapping("/")
    open fun get(): ResponseEntity<HelloData> {
        return ResponseEntity.ok(service.get())
    }

}
