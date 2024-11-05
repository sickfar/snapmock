package org.snapmock.core

import java.io.InputStream
import java.nio.file.Path
import java.util.function.Supplier
import kotlin.io.path.name

/**
 * Source to read snapshot from
 *
 * @see PathSource
 * @see StreamSource
 * @see ClassPathResourceSource
 *
 * @since 1.0.0
 * @author Roman Aksenenko
 */
sealed interface Source {
    /**
     * Source name (if applicable)
     */
    val name: String
}

/**
 * Snapshot source located on a file system
 * @param path File system path to read snapshot from
 *
 * @since 1.0.0
 * @author Roman Aksenenko
 */
data class PathSource(
    /**
     * File system path to read snapshot from
     */
    val path: Path
) : Source {
    override val name: String = path.name
}

/**
 * Snapshot source read from an InputStream
 * @param streamSupplier Supplier of an input stream. Guarantees a repeatable read from a stream
 *
 * @since 1.0.0
 * @author Roman Aksenenko
 */
data class StreamSource(
    /**
     * Supplier of an input stream. Guarantees a repeatable read from a stream
     */
    val streamSupplier: Supplier<InputStream>
) : Source {
    override val name: String = "StreamSource<unknown>"
}

/**
 * Snapshot source located in a class path
 * @param aClass A class, relative of which the resource is located
 * @param resourcePath Relative or absolute classpath resource path
 *
 * @since 1.0.0
 * @author Roman Aksenenko
 */
data class ClassPathResourceSource(
    /**
     * A class, relative of which the resource is located
     */
    val aClass: Class<*>,
    /**
     * Relative or absolute classpath resource path
     */
    val resourcePath: String
) : Source {
    override val name: String = resourcePath
}

/**
 * Representation of a snapshot loaded from a specific source
 * @param source Snapshot source
 * @param snap Loaded snapshot
 *
 * @since 1.0.0
 * @author Roman Aksenenko
 */
data class SnapFromSource(
    /**
     * Snapshot source
     */
    val source: Source,
    /**
     * Loaded snapshot
     */
    val snap: SnapData
)
