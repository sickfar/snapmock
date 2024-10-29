package org.snapmock.snap.core

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.io.path.getPosixFilePermissions
import kotlin.io.path.setPosixFilePermissions

private val log = KotlinLogging.logger {}

class SnapWriter(
    private val directory: Path,
    private val objectMapper: ObjectMapper,
) {

    fun write(snap: SnapData) {
        val now = Instant.now()
        val nowFormatted = DateTimeFormatter.ISO_INSTANT.format(now)
        val fileName = "snap_${snap.main.className}_${snap.main.methodName}_${nowFormatted}.json"
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
            directory.setPosixFilePermissions(perms)
            log.trace { "Permissions $perms set to a directory $directory" }
        }
        val snapFile = directory.resolve(fileName).toAbsolutePath()
        log.trace { "Snap file: $snapFile" }
        Files.newOutputStream(snapFile).use {
            objectMapper.writeValue(it, snap)
            log.info { "Snap file written: $snapFile" }
        }
        // TODO make permissions customizable
        val perms = snapFile.getPosixFilePermissions() + PosixFilePermission.GROUP_READ + PosixFilePermission.OTHERS_READ
        snapFile.setPosixFilePermissions(perms)
        log.trace { "Permissions $perms set to a file $snapFile" }
    }

}

class SnapReader(
    private val objectMapper: ObjectMapper,
) {

    fun read(snapFile: Path): SnapData {
        TODO()
    }

}
