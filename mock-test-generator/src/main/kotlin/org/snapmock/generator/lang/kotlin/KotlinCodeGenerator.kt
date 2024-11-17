package org.snapmock.generator.lang.kotlin

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import org.snapmock.generator.CodeGenerator
import org.snapmock.generator.Mode
import org.snapmock.generator.data.SnapMockTest
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

class KotlinCodeGenerator(
    private val mode: Mode,
    private val output: Path
) : CodeGenerator {
    override fun generate(className: String, methodName: String?, tests: List<SnapMockTest>): Path {
        TODO("Not supported yet")
        val packageName = className.substringBeforeLast('.')
        val simpleClassName = className.substringAfterLast('.')
        val testClassName = when (mode) {
            Mode.PER_CLASS -> simpleClassName + "Test"
            Mode.PER_METHOD -> simpleClassName + methodName!!.replaceFirstChar { it.uppercase() } + "Test"
            Mode.PER_SNAP_FILE -> simpleClassName + methodName!!.replaceFirstChar { it.uppercase() } + "Test"
        }
        val classBuilder = TypeSpec.classBuilder(testClassName)


        val classDir = output.resolve(simpleClassName.replace('.', '/'))
        if (!classDir.exists()) {
            Files.createDirectories(classDir)
        }
        val testFileName = "$testClassName.kt"
        val testFile = classDir.resolve(testFileName)
        check(!testFile.exists()) { "Test file $testFileName already not exist." }
        Files.createFile(testFile)
        FileSpec.builder(packageName, testFileName)
            .addType(classBuilder.build())
            .build()
            .writeTo(testFile)
        return testFile
    }
}
