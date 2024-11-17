package io.github.sickfar.snapmock.spring

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.sickfar.snapmock.core.PathSource
import io.github.sickfar.snapmock.core.SnapData
import io.github.sickfar.snapmock.core.objectMapper
import io.github.sickfar.snapmock.mock.mockito.MockitoMockSupport
import io.github.sickfar.snapmock.spring.data.SpringDataApp
import io.github.sickfar.snapmock.spring.data.author.AuthorDto
import io.github.sickfar.snapmock.spring.data.author.AuthorEntity
import io.github.sickfar.snapmock.spring.data.author.AuthorRepository
import io.github.sickfar.snapmock.spring.data.author.AuthorService
import io.github.sickfar.snapmock.spring.data.book.BookEntity
import io.github.sickfar.snapmock.spring.data.book.BookRepository
import io.github.sickfar.snapmock.spring.data.book.BookService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
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

class SnapSpringDataTest {
    private val objectMapper = objectMapper(null)

    @Test
    fun testSpringDataAppBooksSimple() {
        val dir = Files.createTempDirectory("snap-test").toAbsolutePath().normalize()
        assumeTrue(Files.isDirectory(dir) && Files.exists(dir) && Files.list(dir).count() == 0L)
        val application = SpringApplication(SpringDataApp::class.java)
        application.setDefaultProperties(mapOf("snapmock.snap.directory" to dir.toString()))
        val context = application.run() as ServletWebServerApplicationContext
        val author = context.getBean(AuthorRepository::class.java).save(
            AuthorEntity(
                id = null,
                name = "Test author"
            )
        )
        context.getBean(BookRepository::class.java).save(
            BookEntity(
                id = null,
                title = "Test",
                authors = listOf(author)
            )
        )
        val port = context.webServer.port
        val httpClient = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder().GET().uri(URI.create("http://localhost:$port/books")).build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        SpringApplication.exit(context)
        assertEquals(response.statusCode(), 200)
        val file = Files.list(dir)
            .filter {
                it.name.startsWith("${BookService::class.simpleName}")
                        && it.name.endsWith(".json")
            }
            .findFirst()
            .getOrNull()
        assertNotNull(file)
        val snap = Files.newInputStream(file).use {
            objectMapper.readValue(it, SnapData::class.java)
        }
        assertEquals(BookService::class.qualifiedName, snap.main.className)
        assertEquals("getBooks", snap.main.methodName)
        assertThat(snap.dependents).hasSize(1)
        assertThat(snap.dependents).singleElement()
            .hasFieldOrPropertyWithValue("className", BookRepository::class.qualifiedName)
        assertThat(snap.dependents).singleElement().hasFieldOrPropertyWithValue("methodName", "findAll")

        assertDoesNotThrow {
            MockitoMockSupport.doSnapshotTest(PathSource(file))
        }
        FileSystemUtils.deleteRecursively(dir)
    }

    @Test
    fun testSpringDataAppBooksArray() {
        val dir = Files.createTempDirectory("snap-test").toAbsolutePath().normalize()
        assumeTrue(Files.isDirectory(dir) && Files.exists(dir) && Files.list(dir).count() == 0L)
        val application = SpringApplication(SpringDataApp::class.java)
        application.setDefaultProperties(mapOf("snapmock.snap.directory" to dir.toString()))
        val context = application.run() as ServletWebServerApplicationContext
        val author = context.getBean(AuthorRepository::class.java).save(
            AuthorEntity(
                id = null,
                name = "Test author"
            )
        )
        context.getBean(BookRepository::class.java).save(
            BookEntity(
                id = null,
                title = "Test",
                authors = listOf(author)
            )
        )
        val port = context.webServer.port
        val httpClient = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder().GET().uri(URI.create("http://localhost:$port/books/array")).build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        SpringApplication.exit(context)
        assertEquals(response.statusCode(), 200)
        val file = Files.list(dir)
            .filter {
                it.name.startsWith("${BookService::class.simpleName}")
                        && it.name.endsWith(".json")
            }
            .findFirst()
            .getOrNull()
        assertNotNull(file)
        val snap = Files.newInputStream(file).use {
            objectMapper.readValue(it, SnapData::class.java)
        }
        assertEquals(BookService::class.qualifiedName, snap.main.className)
        assertEquals("getBooksArray", snap.main.methodName)
        assertThat(snap.dependents).hasSize(1)
        assertThat(snap.dependents).singleElement()
            .hasFieldOrPropertyWithValue("className", BookRepository::class.qualifiedName)
        assertThat(snap.dependents).singleElement().hasFieldOrPropertyWithValue("methodName", "findAllAsArray")

        assertDoesNotThrow {
            MockitoMockSupport.doSnapshotTest(PathSource(file))
        }
        FileSystemUtils.deleteRecursively(dir)
    }

    @Test
    fun testSpringDataApp_IgnorePlatformTransactionManager() {
        val dir = Files.createTempDirectory("snap-test").toAbsolutePath().normalize()
        assumeTrue(Files.isDirectory(dir) && Files.exists(dir) && Files.list(dir).count() == 0L)
        val application = SpringApplication(SpringDataApp::class.java)
        application.setDefaultProperties(mapOf("snapmock.snap.directory" to dir.toString()))
        val context = application.run() as ServletWebServerApplicationContext
        val port = context.webServer.port
        val httpClient = HttpClient.newHttpClient()
        val body = HttpRequest.BodyPublishers.ofString(
            context.getBean(ObjectMapper::class.java).writeValueAsString(
                AuthorDto(
                    id = null,
                    name = "New author"
                )
            )
        )
        val request = HttpRequest.newBuilder().POST(body).uri(URI.create("http://localhost:$port/authors")).header("Content-Type", "application/json").build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        SpringApplication.exit(context)
        assertEquals(response.statusCode(), 200)
        val file = Files.list(dir)
            .filter {
                it.name.startsWith("${AuthorService::class.simpleName}")
                        && it.name.endsWith(".json")
            }
            .findFirst()
            .getOrNull()
        assertNotNull(file)
        val snap = Files.newInputStream(file).use {
            objectMapper.readValue(it, SnapData::class.java)
        }
        assertEquals(AuthorService::class.qualifiedName, snap.main.className)
        assertEquals("create", snap.main.methodName)
        assertThat(snap.dependents).hasSize(1)
        assertThat(snap.dependents).singleElement()
            .hasFieldOrPropertyWithValue("className", AuthorRepository::class.qualifiedName)
        assertThat(snap.dependents).singleElement().hasFieldOrPropertyWithValue("methodName", "save")

        assertDoesNotThrow {
            MockitoMockSupport.doSnapshotTest(PathSource(file))
        }
        FileSystemUtils.deleteRecursively(dir)
    }

}
