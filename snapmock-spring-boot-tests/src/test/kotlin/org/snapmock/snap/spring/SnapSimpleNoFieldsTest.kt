package org.snapmock.snap.spring

import org.assertj.core.api.Assertions.`as`
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.InstanceOfAssertFactories
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.snapmock.core.PathSource
import org.snapmock.core.SnapData
import org.snapmock.core.objectMapper
import org.snapmock.mock.mockito.MockitoMockSupport
import org.snapmock.snap.spring.simple.SimpleApp
import org.snapmock.snap.spring.simple.hello.HelloData
import org.snapmock.snap.spring.simple.hello.HelloDataProvider
import org.snapmock.snap.spring.simple.hello.HelloFactory
import org.snapmock.snap.spring.simple.hello.HelloRepository
import org.snapmock.snap.spring.simple.hello.HelloService
import org.springframework.boot.SpringApplication
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext
import org.springframework.util.FileSystemUtils
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.ByteArrayOutputStream
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

class SnapSimpleNoFieldsTest {

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
            .filter {
                it.name.startsWith("${HelloService::class.simpleName}")
                        && it.name.endsWith(".json")
            }
            .findFirst()
            .getOrNull()
        assertNotNull(file)
        val snap = Files.newInputStream(file).use {
            objectMapper.readValue(it, SnapData::class.java)
        }
        assertEquals(HelloService::class.qualifiedName, snap.main.className)
        assertEquals("get", snap.main.methodName)
        assertThat(snap.dependents).hasSize(1)
        assertThat(snap.dependents).singleElement()
            .hasFieldOrPropertyWithValue("className", HelloRepository::class.qualifiedName)
        assertThat(snap.dependents).singleElement().hasFieldOrPropertyWithValue("methodName", "getMessage")

        assertDoesNotThrow {
            MockitoMockSupport.doSnapshotTest(PathSource(file))
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
        val request = HttpRequest.newBuilder().POST(BodyPublishers.ofString("Hello test"))
            .uri(URI.create("http://localhost:$port/")).build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        SpringApplication.exit(context)
        assertEquals(response.statusCode(), 200)
        val file = Files.list(dir)
            .filter {
                it.name.startsWith("${HelloService::class.simpleName}")
                        && it.name.endsWith(".json")
            }
            .findFirst()
            .getOrNull()
        assertNotNull(file)
        val snap = Files.newInputStream(file).use {
            objectMapper.readValue(it, SnapData::class.java)
        }
        assertEquals(HelloService::class.qualifiedName, snap.main.className)
        assertEquals("post", snap.main.methodName)
        assertThat(snap.dependents).hasSize(1).singleElement()
            .hasFieldOrPropertyWithValue("className", HelloRepository::class.qualifiedName)
            .hasFieldOrPropertyWithValue("methodName", "post")
        assertThat(snap.dependents).singleElement().extracting("arguments", `as`(InstanceOfAssertFactories.LIST))
            .hasSize(1).singleElement()
            .isEqualTo("Hello test")
        assertThat(snap.dependents).singleElement().extracting("result")
            .hasFieldOrPropertyWithValue("data", "Hello test")

        assertDoesNotThrow {
            MockitoMockSupport.doSnapshotTest(PathSource(file))
        }
        FileSystemUtils.deleteRecursively(dir)
    }

    @ParameterizedTest
    @EnumSource(HelloFactory::class)
    fun testFactoryBy(factory: HelloFactory) {
        val dir = Files.createTempDirectory("snap-test").toAbsolutePath().normalize()
        assumeTrue(Files.isDirectory(dir) && Files.exists(dir) && Files.list(dir).count() == 0L)
        val application = SpringApplication(SimpleApp::class.java)
        application.setDefaultProperties(mapOf("snapmock.snap.directory" to dir.toString()))
        val context = application.run() as ServletWebServerApplicationContext
        val port = context.webServer.port
        val httpClient = HttpClient.newHttpClient()
        val request =
            HttpRequest.newBuilder().GET().uri(URI.create("http://localhost:$port/factory/${factory.name}")).build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        SpringApplication.exit(context)
        assertEquals(response.statusCode(), 200)
        assertThat(response.body()).contains("Hello from $factory")
        val file = Files.list(dir)
            .filter {
                it.name.startsWith("${HelloService::class.simpleName}")
                        && it.name.endsWith(".json")
            }
            .findFirst()
            .getOrNull()
        assertNotNull(file)
        val snap = Files.newInputStream(file).use {
            objectMapper.readValue(it, SnapData::class.java)
        }
        assertEquals(HelloService::class.qualifiedName, snap.main.className)
        assertEquals("getByFactory", snap.main.methodName)
        assertThat(snap.factories).hasSize(1).singleElement()
            .hasFieldOrPropertyWithValue(
                "className",
                "org.snapmock.snap.spring.simple.hello.HelloProviderFactoryBy" + factory.name.lowercase()
                    .replaceFirstChar { it.uppercase() })
        assertThat(snap.factories).singleElement()
            .hasFieldOrPropertyWithValue("methodName", "getProvider")
        assertThat(snap.dependents).hasSize(1).singleElement()
            .hasFieldOrPropertyWithValue("className", HelloDataProvider::class.qualifiedName)
        assertThat(snap.dependents).hasSize(1).singleElement()
            .hasFieldOrPropertyWithValue("methodName", "getGreeting")

        assertDoesNotThrow {
            MockitoMockSupport.doSnapshotTest(PathSource(file))
        }
        FileSystemUtils.deleteRecursively(dir)
    }

    @Test
    fun testResultNull() {
        val dir = Files.createTempDirectory("snap-test").toAbsolutePath().normalize()
        assumeTrue(Files.isDirectory(dir) && Files.exists(dir) && Files.list(dir).count() == 0L)
        val application = SpringApplication(SimpleApp::class.java)
        application.setDefaultProperties(mapOf("snapmock.snap.directory" to dir.toString()))
        val context = application.run() as ServletWebServerApplicationContext
        val port = context.webServer.port
        val httpClient = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder().GET().uri(URI.create("http://localhost:$port/null")).build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        SpringApplication.exit(context)
        assertEquals(response.statusCode(), 200)
        val file = Files.list(dir)
            .filter {
                it.name.startsWith("${HelloService::class.simpleName}")
                        && it.name.endsWith(".json")
            }
            .findFirst()
            .getOrNull()
        assertNotNull(file)
        val snap = Files.newInputStream(file).use {
            objectMapper.readValue(it, SnapData::class.java)
        }
        assertEquals(HelloService::class.qualifiedName, snap.main.className)
        assertEquals("getNull", snap.main.methodName)
        assertThat(snap.dependents).hasSize(0)

        assertDoesNotThrow {
            MockitoMockSupport.doSnapshotTest(PathSource(file))
        }
        FileSystemUtils.deleteRecursively(dir)
    }

    @Test
    fun testDepResultAndArgNull() {
        val dir = Files.createTempDirectory("snap-test").toAbsolutePath().normalize()
        assumeTrue(Files.isDirectory(dir) && Files.exists(dir) && Files.list(dir).count() == 0L)
        val application = SpringApplication(SimpleApp::class.java)
        application.setDefaultProperties(mapOf("snapmock.snap.directory" to dir.toString()))
        val context = application.run() as ServletWebServerApplicationContext
        val port = context.webServer.port
        val httpClient = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder().GET().uri(URI.create("http://localhost:$port/null/dep")).build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        SpringApplication.exit(context)
        assertEquals(response.statusCode(), 200)
        val file = Files.list(dir)
            .filter {
                it.name.startsWith("${HelloService::class.simpleName}")
                        && it.name.endsWith(".json")
            }
            .findFirst()
            .getOrNull()
        assertNotNull(file)
        val snap = Files.newInputStream(file).use {
            objectMapper.readValue(it, SnapData::class.java)
        }
        assertEquals(HelloService::class.qualifiedName, snap.main.className)
        assertEquals("getDepNull", snap.main.methodName)
        assertThat(snap.dependents).hasSize(1)
        assertThat(snap.dependents).singleElement()
            .hasFieldOrPropertyWithValue("className", HelloRepository::class.qualifiedName)
        assertThat(snap.dependents).singleElement().hasFieldOrPropertyWithValue("methodName", "getNull")
        assertThat(snap.dependents).singleElement().extracting("arguments", `as`(InstanceOfAssertFactories.LIST))
            .hasSize(1).singleElement().isNull()

        assertDoesNotThrow {
            MockitoMockSupport.doSnapshotTest(PathSource(file))
        }
        FileSystemUtils.deleteRecursively(dir)
    }

    @Test
    fun testStreamingResponseBodyPostprocessor() {
        val dir = Files.createTempDirectory("snap-test").toAbsolutePath().normalize()
        assumeTrue(Files.isDirectory(dir) && Files.exists(dir) && Files.list(dir).count() == 0L)
        val application = SpringApplication(SimpleApp::class.java)
        application.setDefaultProperties(mapOf("snapmock.snap.directory" to dir.toString()))
        val context = application.run() as ServletWebServerApplicationContext
        val port = context.webServer.port
        val httpClient = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder().GET().uri(URI.create("http://localhost:$port/streaming")).build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray())
        println(String(response.body()))
        SpringApplication.exit(context)
        assertEquals(response.statusCode(), 200)
        val file = Files.list(dir)
            .filter {
                it.name.startsWith("${HelloService::class.simpleName}")
                        && it.name.endsWith(".json")
            }
            .findFirst()
            .getOrNull()
        assertNotNull(file)
        val snap = Files.newInputStream(file).use {
            objectMapper.readValue(it, SnapData::class.java)
        }
        assertEquals(HelloService::class.qualifiedName, snap.main.className)
        assertEquals("getStreaming", snap.main.methodName)
        assertThat(snap.dependents).hasSize(1)
        assertThat(snap.dependents).singleElement()
            .hasFieldOrPropertyWithValue("className", HelloRepository::class.qualifiedName)
        assertThat(snap.dependents).singleElement().hasFieldOrPropertyWithValue("methodName", "getMessage")

        assertDoesNotThrow {
            MockitoMockSupport.doSnapshotTest(PathSource(file), { null }) { expected: Nothing?, actual: StreamingResponseBody? ->
                assertNotNull(actual)
                val stream = ByteArrayOutputStream()
                actual.writeTo(stream)
                val data = objectMapper.readValue(stream.toByteArray(), HelloData::class.java)
                assertEquals("Hello World", data.data)
            }
        }
        FileSystemUtils.deleteRecursively(dir)
    }

}
