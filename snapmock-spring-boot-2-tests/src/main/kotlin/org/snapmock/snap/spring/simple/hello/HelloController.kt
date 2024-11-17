package org.snapmock.snap.spring.simple.hello

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody

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

    @GetMapping("/factory/{factory}")
    fun getByFactory(@PathVariable("factory") factory: HelloFactory): ResponseEntity<HelloData> {
        return ResponseEntity.ok(service.getByFactory(factory))
    }

    @GetMapping("/null")
    fun getNull(): ResponseEntity<HelloData?> {
        return ResponseEntity.ok(service.getNull())
    }

    @GetMapping("/null/dep")
    fun getDepNull(): ResponseEntity<HelloData?> {
        return ResponseEntity.ok(service.getDepNull())
    }

    @GetMapping("/streaming")
    fun streaming(): ResponseEntity<StreamingResponseBody> {
        return ResponseEntity.ok(service.getStreaming())
    }

}
