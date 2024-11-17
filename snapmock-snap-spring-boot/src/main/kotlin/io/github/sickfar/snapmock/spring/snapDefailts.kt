package io.github.sickfar.snapmock.spring

import java.nio.file.Path
import java.nio.file.Paths

fun defaultDirectory(): Path = Paths.get("./snap")

fun defaultIgnoreClasses(): List<String> = listOf(
    "org.springframework.transaction.PlatformTransactionManager"
)
