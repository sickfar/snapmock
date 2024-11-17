package io.github.sickfar.snapmock.core

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.io.path.getPosixFilePermissions
import kotlin.io.path.setPosixFilePermissions

private val log = KotlinLogging.logger {}

/**
 * Writer to serialize and write a snapshot to a specified output directory
 * @param directory Output directory to write snapshots
 * @param objectMapper Configured Jackson ObjectMapper to serialize snapshots
 *
 * @see SnapReader
 * @since 1.0.0
 * @author Roman Aksenenko
 */
class SnapWriter(
    /**
     * Output directory to write snapshots
     */
    private val directory: Path,
    /**
     * Configured ObjectMapper to serialize snapshots
     */
    private val objectMapper: ObjectMapper,
) {

    /**
     * Serialize and write a snapshot
     * @param snap Snapshot to write
     */
    fun write(snap: SnapData) {
        val now = Instant.now()
        val nowFormatted = DateTimeFormatter.ISO_INSTANT.format(now)
        val fileName = "${snap.main.className.substringAfterLast(".")}_${snap.main.methodName}_${nowFormatted}.json"
            .replace("[^a-zA-Z0-9.\\-]", "_")
        log.trace { "Snap writing to $fileName" }
        if (Files.notExists(directory)) {
            Files.createDirectories(directory)
            log.debug { "Directory created: $directory" }
            val perms = directory.getPosixFilePermissions() + setOf(
                PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.OTHERS_EXECUTE,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.OTHERS_READ
            )
            try {
                directory.setPosixFilePermissions(perms)
            } catch (e: Exception) {
                log.warn { "Cannot set permissions for $directory: ${e.message}" }
            }
            log.trace { "Permissions $perms set to a directory $directory" }
        }
        val snapFile = directory.resolve(fileName).toAbsolutePath()
        log.trace { "Snap file: $snapFile" }
        Files.newOutputStream(snapFile).use {
            objectMapper.writeValue(it, snap)
            log.info { "Snap file written: $snapFile" }
        }
        // TODO make permissions customizable
        val perms =
            snapFile.getPosixFilePermissions() + PosixFilePermission.GROUP_READ + PosixFilePermission.OTHERS_READ
        try {
            snapFile.setPosixFilePermissions(perms)
        } catch (e: Exception) {
            log.warn { "Cannot set permissions for $directory: ${e.message}" }
        }
        log.trace { "Permissions $perms set to a file $snapFile" }
    }

}

/**
 * Get an InputStream from a source
 * @param source Snapshot source to convert to InputStream
 * @return Constructed InputStream
 *
 * @see Source
 * @since 1.0.0
 * @author Roman Aksenenko
 */
fun inputStream(source: Source): InputStream {
    return when (source) {
        is StreamSource -> source.streamSupplier.get()
        is PathSource -> Files.newInputStream(source.path)
        is ClassPathResourceSource -> checkNotNull(source.aClass.getResourceAsStream(source.resourcePath)) {
            "Resource ${source.resourcePath} does not exist"
        }
    }
}

/**
 * Reader to read and deserialize snapshots
 * @param objectMapper Configured Jackson ObjectMapper
 *
 * @see SnapWriter
 * @since 1.0.0
 * @author Roman Aksenenko
 */
class SnapReader(
    private val objectMapper: ObjectMapper,
) {

    /**
     * Read a snapshot from a [Source]
     * @param source Source to read a snapshot from
     * @return [SnapFromSource] which contains read source and snapshot
     */
    fun read(source: Source): SnapFromSource {
        val snap = inputStream(source).use { readStream(it) }
        return SnapFromSource(source, snap)
    }

    private fun readStream(it: InputStream): SnapData =
        objectMapper.readValue(it, SnapData::class.java)

}
