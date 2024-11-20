package io.github.sickfar.snapmock.spring

import io.github.sickfar.snapmock.core.PathSource
import io.github.sickfar.snapmock.core.SnapData
import io.github.sickfar.snapmock.core.objectMapper
import io.github.sickfar.snapmock.mock.mockito.MockitoMockSupport
import io.github.sickfar.snapmock.spring.simple.SimpleApp
import io.github.sickfar.snapmock.spring.simple.hello.HelloDataProvider
import io.github.sickfar.snapmock.spring.simple.hello.HelloFactory
import io.github.sickfar.snapmock.spring.simple.hello.HelloProviderFactoryByClass
import io.github.sickfar.snapmock.spring.simple.hello.HelloProviderFactoryByField
import io.github.sickfar.snapmock.spring.simple.hello.HelloProviderFactoryByMethod
import io.github.sickfar.snapmock.spring.simple.hello.HelloProviderFactoryBySetter
import io.github.sickfar.snapmock.spring.simple.hello.HelloRepository
import io.github.sickfar.snapmock.spring.simple.hello.HelloService
import org.assertj.core.api.Assertions.`as`
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.InstanceOfAssertFactories
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
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

    @Mock
    lateinit var factoryByClass: HelloProviderFactoryByClass

    @Mock
    lateinit var factoryByMethod: HelloProviderFactoryByMethod

    @Mock
    lateinit var factoryBySetter: HelloProviderFactoryBySetter

    @Mock
    lateinit var factoryByField: HelloProviderFactoryByField

    @Mock
    lateinit var helloDataProvider: HelloDataProvider

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
        assertThat(snap.dependents).hasSize(1)
        assertThat(snap.dependents).singleElement().hasFieldOrPropertyWithValue("className", HelloRepository::class.qualifiedName)
        assertThat(snap.dependents).singleElement().hasFieldOrPropertyWithValue("methodName", "getMessage")

        assertDoesNotThrow {
            MockitoMockSupport.doSnapshotTestFromFields(this, PathSource(file))
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
        assertThat(snap.dependents).hasSize(1).singleElement()
            .hasFieldOrPropertyWithValue("className", HelloRepository::class.qualifiedName)
            .hasFieldOrPropertyWithValue("methodName", "post")
        assertThat(snap.dependents).singleElement().extracting("arguments", `as`(InstanceOfAssertFactories.LIST))
            .hasSize(1).singleElement()
            .isEqualTo("Hello test")
        assertThat(snap.dependents).singleElement().extracting("result")
            .hasFieldOrPropertyWithValue("data", "Hello test")

        assertDoesNotThrow {
            MockitoMockSupport.doSnapshotTestFromFields(this, PathSource(file))
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
        val request = HttpRequest.newBuilder().GET().uri(URI.create("http://localhost:$port/factory/${factory.name}")).build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        SpringApplication.exit(context)
        assertEquals(response.statusCode(), 200)
        assertThat(response.body()).contains("Hello from $factory")
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
        assertEquals("getByFactory", snap.main.methodName)
        assertThat(snap.factories).hasSize(1).singleElement()
            .hasFieldOrPropertyWithValue("className", "io.github.sickfar.snapmock.spring.simple.hello.HelloProviderFactoryBy" + factory.name.lowercase().replaceFirstChar { it.uppercase() })
        assertThat(snap.factories).singleElement()
            .hasFieldOrPropertyWithValue("methodName", "getProvider")
        assertThat(snap.dependents).hasSize(1).singleElement()
            .hasFieldOrPropertyWithValue("className", HelloDataProvider::class.qualifiedName)
        assertThat(snap.dependents).hasSize(1).singleElement()
            .hasFieldOrPropertyWithValue("methodName", "getGreeting")

        assertDoesNotThrow {
            // mockito does not inject setters and fields
            helloService.factoryBySetter = factoryBySetter
            helloService.factoryByField = factoryByField
            MockitoMockSupport.doSnapshotTestFromFields(this, PathSource(file))
        }
        FileSystemUtils.deleteRecursively(dir)
    }

}
