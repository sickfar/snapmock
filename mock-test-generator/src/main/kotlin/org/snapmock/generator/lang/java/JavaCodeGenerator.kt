package org.snapmock.generator.lang.java

import com.palantir.javapoet.JavaFile
import com.palantir.javapoet.MethodSpec
import com.palantir.javapoet.TypeName
import com.palantir.javapoet.TypeSpec
import org.snapmock.generator.CodeGenerator
import org.snapmock.generator.data.SnapMockTest
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists

class JavaCodeGenerator : CodeGenerator {

    override fun generate(output: Path, forClass: String, snaps: List<SnapMockTest>): Path {
        val packageName = forClass.substringBeforeLast('.')
        val className = forClass.substringAfterLast('.')
        val testClassName = className + "Test"
        val testClassBuilder = TypeSpec.classBuilder(testClassName)


        snaps.forEachIndexed { index, test ->
            val testMethodName = test.testMethodAttributes.name
            val testMethod = MethodSpec.methodBuilder("test" + testMethodName.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            } + index)
                .returns(TypeName.VOID)
                .build()
            testClassBuilder.addMethod(testMethod)
        }

        val classDir = output.resolve(className.replace('.', '/'))
        if (!classDir.exists()) {
            Files.createDirectories(classDir)
        }
        val testFileName = "$testClassName.java"
        val testFile = classDir.resolve(testFileName)
        check(!testFile.exists()) { "Test file $testFileName already not exist." }
        Files.createFile(testFile)
        val javaFile = JavaFile.builder(packageName, testClassBuilder.build())
            .build()
        javaFile.writeTo(testFile)
        return testFile
    }
}
