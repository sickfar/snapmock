package org.snapmock.snap.spring

import org.assertj.core.api.Assertions.`as`
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.InstanceOfAssertFactories
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.snapmock.core.PathSource
import org.snapmock.core.SnapData
import org.snapmock.core.objectMapper
import org.snapmock.mock.mockito.MockitoMockSupport
import org.snapmock.snap.spring.simple.SimpleApp
import org.snapmock.snap.spring.simple.hello.HelloRepository
import org.snapmock.snap.spring.simple.hello.HelloService
import org.springframework.boot.SpringApplication
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext
import org.springframework.util.FileSystemUtils
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse
import java.nio.file.Files
import kotlin.io.path.name
import kotlin.jvm.optionals.getOrNull
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(MockitoExtension::class)
class SnapSimpleTest {

    @Mock
    lateinit var helloRepository: HelloRepository

    @InjectMocks
    lateinit var helloService: HelloService

    private val objectMapper = objectMapper(null)

    @Test
    fun testNoArs() {
        val dir = Files.createTempDirectory("snap-test").toAbsolutePath().normalize()
        assumeTrue(Files.isDirectory(dir) && Files.exists(dir) && Files.list(dir).count() == 0L)
        val application = SpringApplication(SimpleApp::class.java)
        application.setDefaultProperties(mapOf("snapmock.snap.directory" to dir.toString()))
        val context = application.run() as ServletWebServerApplicationContext
        val port = context.webServer.port
        val httpClient = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder().GET().uri(URI.create("http://localhost:$port/")).build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        SpringApplication.exit(context)
        assertEquals(response.statusCode(), 200)
        val file = Files.list(dir)
            .filter { it.name.startsWith("${HelloService::class.simpleName}")
                    && it.name.endsWith(".json") }
            .findFirst()
            .getOrNull()
        assertNotNull(file)
        val snap = Files.newInputStream(file).use {
            objectMapper.readValue(it, SnapData::class.java)
        }
        assertEquals(HelloService::class.qualifiedName, snap.main.className)
        assertEquals("get", snap.main.methodName)
        assertThat(snap.dependencies).hasSize(1)
        assertThat(snap.dependencies).singleElement().hasFieldOrPropertyWithValue("className", HelloRepository::class.qualifiedName)
        assertThat(snap.dependencies).singleElement().hasFieldOrPropertyWithValue("methodName", "getMessage")

        assertDoesNotThrow {
            MockitoMockSupport.doSnapshotTest(this, PathSource(file))
        }
        FileSystemUtils.deleteRecursively(dir)
    }

    @Test
    fun testOneSimpleArg() {
        val dir = Files.createTempDirectory("snap-test").toAbsolutePath().normalize()
        assumeTrue(Files.isDirectory(dir) && Files.exists(dir) && Files.list(dir).count() == 0L)
        val application = SpringApplication(SimpleApp::class.java)
        application.setDefaultProperties(mapOf("snapmock.snap.directory" to dir.toString()))
        val context = application.run() as ServletWebServerApplicationContext
        val port = context.webServer.port
        val httpClient = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder().POST(BodyPublishers.ofString("Hello test")).uri(URI.create("http://localhost:$port/")).build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        SpringApplication.exit(context)
        assertEquals(response.statusCode(), 200)
        val file = Files.list(dir)
            .filter { it.name.startsWith("${HelloService::class.simpleName}")
                    && it.name.endsWith(".json") }
            .findFirst()
            .getOrNull()
        assertNotNull(file)
        val snap = Files.newInputStream(file).use {
            objectMapper.readValue(it, SnapData::class.java)
        }
        assertEquals(HelloService::class.qualifiedName, snap.main.className)
        assertEquals("post", snap.main.methodName)
        assertThat(snap.dependencies).hasSize(1).singleElement()
            .hasFieldOrPropertyWithValue("className", HelloRepository::class.qualifiedName)
            .hasFieldOrPropertyWithValue("methodName", "post")
        assertThat(snap.dependencies).singleElement().extracting("arguments", `as`(InstanceOfAssertFactories.LIST))
            .hasSize(1).singleElement()
            .isEqualTo("Hello test")
        assertThat(snap.dependencies).singleElement().extracting("result")
            .hasFieldOrPropertyWithValue("data", "Hello test")

        assertDoesNotThrow {
            MockitoMockSupport.doSnapshotTest(this, PathSource(file))
        }
        FileSystemUtils.deleteRecursively(dir)
    }

}
