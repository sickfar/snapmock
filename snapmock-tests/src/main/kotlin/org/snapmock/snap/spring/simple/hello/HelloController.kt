package org.snapmock.snap.spring.simple.hello

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController(value = "/hello")
class HelloController(
    private val service: HelloService
) {

    @GetMapping("/")
    fun get(): ResponseEntity<HelloData> {
        return ResponseEntity.ok(service.get())
    }

    @PostMapping("/")
    fun post(@RequestBody data: String): ResponseEntity<HelloData> {
        return ResponseEntity.ok(service.post(data))
    }

}
