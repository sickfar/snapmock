package org.snapmock.generator.lang.kotlin

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import org.snapmock.generator.CodeGenerator
import org.snapmock.generator.data.SnapMockTest
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

class KotlinCodeGenerator: CodeGenerator {
    override fun generate(output: Path, forClass: String, snaps: List<SnapMockTest>): Path {
        val packageName = forClass.substringBeforeLast('.')
        val className = forClass.substringAfterLast('.')
        val testClassName = className + "Test"
        val classBuilder = TypeSpec.classBuilder(testClassName)


        val classDir = output.resolve(className.replace('.', '/'))
        if (!classDir.exists()) {
            Files.createDirectories(classDir)
        }
        val testFileName = "$testClassName.java"
        val testFile = classDir.resolve(testFileName)
        check(!testFile.exists()) { "Test file $testFileName already not exist." }
        Files.createFile(testFile)
        FileSpec.builder(packageName, testFileName)
            .addType(classBuilder.build())
            .build()
            .writeTo(testFile)

        TODO()
        return testFile
    }
}
