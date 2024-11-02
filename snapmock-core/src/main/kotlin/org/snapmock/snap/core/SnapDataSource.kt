package org.snapmock.snap.core

import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.name

sealed interface Source {
    val name: String
}

data class PathSource(val path: Path) : Source {
    override val name: String = path.name
}

data class StreamSource(val stream: InputStream) : Source {
    override val name: String = "StreamSource<unknown>"
}

data class ClassPathResourceSource(val resourcePath: String) : Source {
    override val name: String = resourcePath
}

data class SnapDataSource(
    val source: Source,
    val snap: SnapData
)
