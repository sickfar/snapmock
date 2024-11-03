package org.snapmock.snap.core

import java.io.InputStream
import java.nio.file.Path
import java.util.function.Supplier
import kotlin.io.path.name

sealed interface Source {
    val name: String
}

data class PathSource(val path: Path) : Source {
    override val name: String = path.name
}

data class StreamSource(val streamSupplier: Supplier<InputStream>) : Source {
    override val name: String = "StreamSource<unknown>"
}

data class ClassPathResourceSource(
    val aClass: Class<*>,
    val resourcePath: String
) : Source {
    override val name: String = resourcePath
}

data class SnapFromSource(
    val source: Source,
    val snap: SnapData
)
