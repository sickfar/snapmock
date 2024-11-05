package org.snapmock.snap.spring.simple

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.snapmock.core.SnapData
import org.snapmock.core.objectMapper
import org.snapmock.snap.spring.simple.app.HelloController
import org.snapmock.snap.spring.simple.app.HelloService
import org.snapmock.snap.spring.simple.app.SimpleApp
import org.springframework.boot.SpringApplication
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext
import org.springframework.util.FileSystemUtils
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import kotlin.io.path.name
import kotlin.jvm.optionals.getOrNull
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SnapSimpleTest {

    private val objectMapper = objectMapper(null)

    @Test
    fun snapTest() {
        val dir = Files.createTempDirectory("snap-test").toAbsolutePath().normalize()
        assumeTrue(Files.isDirectory(dir) && Files.exists(dir) && Files.list(dir).count() == 0L)
        val application = SpringApplication(SimpleApp::class.java)
        application.setDefaultProperties(mapOf("snapmock.snap.directory" to dir.toString()))
        val context = application.run() as ServletWebServerApplicationContext
        val port = context.webServer.port
        val httpClient = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder().GET().uri(URI.create("http://localhost:$port/")).build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        assertEquals(response.statusCode(), 200)
        val file = Files.list(dir)
            .filter { it.name.startsWith("${HelloController::class.simpleName}")
                    && it.name.endsWith(".json") }
            .findFirst()
            .getOrNull()
        assertNotNull(file)
        val snap = Files.newInputStream(file).use {
            objectMapper.readValue(it, SnapData::class.java)
        }
        assertEquals(HelloController::class.qualifiedName, snap.main.className)
        assertEquals("get", snap.main.methodName)
        assertThat(snap.dependencies).hasSize(1)
        assertThat(snap.dependencies).singleElement().hasFieldOrPropertyWithValue("className", HelloService::class.qualifiedName)
        assertThat(snap.dependencies).singleElement().hasFieldOrPropertyWithValue("methodName", "get")
        SpringApplication.exit(context)
        FileSystemUtils.deleteRecursively(dir)
    }

}
