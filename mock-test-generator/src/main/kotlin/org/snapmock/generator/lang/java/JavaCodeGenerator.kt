package org.snapmock.generator.lang.java

import com.palantir.javapoet.AnnotationSpec
import com.palantir.javapoet.ClassName
import com.palantir.javapoet.CodeBlock
import com.palantir.javapoet.FieldSpec
import com.palantir.javapoet.JavaFile
import com.palantir.javapoet.MethodSpec
import com.palantir.javapoet.TypeName
import com.palantir.javapoet.TypeSpec
import org.snapmock.core.ClassPathResourceSource
import org.snapmock.core.Source
import org.snapmock.core.TestSupport
import org.snapmock.core.inputStream
import org.snapmock.generator.CodeGenerator
import org.snapmock.generator.Mode
import org.snapmock.generator.data.SnapMockTest
import org.snapmock.generator.lang.common.ClassAnnotationExpression
import org.snapmock.generator.lang.common.Field
import org.snapmock.generator.lang.common.JvmAnnotation
import org.snapmock.generator.lang.common.StringAnnotationExpression
import java.nio.file.Files
import java.nio.file.Path
import javax.lang.model.element.Modifier

class JavaCodeGenerator(
    private val mode: Mode,
    output: Path
) : CodeGenerator {

    private val codeDirectory: Path = output.resolve("java")
    private val resourcesDirectory: Path = output.resolve("resources")

    init {
        if (!Files.exists(codeDirectory)) {
            Files.createDirectories(codeDirectory)
        }
        if (!Files.exists(resourcesDirectory)) {
            Files.createDirectories(resourcesDirectory)
        }
    }

    override fun generate(className: String, methodName: String?, tests: List<SnapMockTest>): Path {
        val packageName = className.substringBeforeLast('.')
        val simpleClassName = className.substringAfterLast('.')
        val testClassName = when (mode) {
            Mode.PER_CLASS -> simpleClassName + "Test"
            Mode.PER_METHOD -> simpleClassName + methodName!!.replaceFirstChar { it.uppercase() } + "Test"
            Mode.PER_SNAP_FILE -> simpleClassName + methodName!!.replaceFirstChar { it.uppercase() } + "Test"
        }
        val testClassBuilder = TypeSpec.classBuilder(testClassName)
        tests.stream().flatMap { it.testClassAnnotations.stream() }.distinct().forEach {
            testClassBuilder.addAnnotation(buildAnnotation(it))
        }

        testClassBuilder.addField(buildField(tests.first().subject))

        tests.stream().flatMap { it.dependencies.stream() }.distinct().forEach {
            testClassBuilder.addField(buildField(it))
        }

        tests.forEachIndexed { index, test ->
            val testMethodName = test.subjectMethod
            val testMethodBuilder =
                MethodSpec.methodBuilder("test" + testMethodName.replaceFirstChar { it.uppercase() } + index)
                    .returns(TypeName.VOID)
            test.testMethodAnnotations.forEach {
                testMethodBuilder.addAnnotation(buildAnnotation(it))
            }
            // create source var
            val resourcePath = packageName.replace('.', '/')
            val resourceName = "%s_%s_%s.json".format(simpleClassName, methodName, index)

            testMethodBuilder.addStatement(CodeBlock.of(
                "final var source = new \$T(\$L.class, \$S)",
                ClassPathResourceSource::class.java,
                testClassName,
                resourceName
            ))

            test.mocks.forEach { mock ->
                testMethodBuilder.addStatement(buildCodeBlockFromExpression(mock.expression))
            }
            test.assertion.statements.forEach { statement ->
                testMethodBuilder.addStatement(buildCodeBlockFromExpression(statement))
            }
            testClassBuilder.addMethod(testMethodBuilder.build())
            writeTestResource(test.source, resourcePath, resourceName)
        }
        val javaFileBuilder = JavaFile.builder(packageName, testClassBuilder.build())
        javaFileBuilder.addStaticImport(TestSupport::class.java, "*")
        tests.stream().flatMap { it.statics.stream() }.distinct().forEach {
            javaFileBuilder.addStaticImport(ClassName.bestGuess(it), "*")
        }
        val written = javaFileBuilder.build().writeToPath(codeDirectory)
        return written
    }

    private fun buildAnnotation(it: JvmAnnotation): AnnotationSpec? {
        val annotationSpecBuilder = AnnotationSpec.builder(ClassName.bestGuess(it.className))
        it.members.forEach { (member, value) ->
            when (value) {
                is StringAnnotationExpression -> annotationSpecBuilder.addMember(member, "\$S", value.value)
                is ClassAnnotationExpression -> annotationSpecBuilder.addMember(
                    member,
                    "\$T.class",
                    ClassName.bestGuess(value.className)
                )
            }
        }
        val annotation = annotationSpecBuilder.build()
        return annotation
    }

    private fun buildField(field: Field): FieldSpec {
        val modifiers =
            field.modifiers.stream().map { Modifier.valueOf(it.uppercase()) }.toArray<Modifier> { arrayOfNulls(it) }
        val fieldSpecBuilder = FieldSpec.builder(ClassName.bestGuess(field.typeName), field.name, *modifiers)
            .addAnnotations(field.annotations.map { buildAnnotation(it) })
        field.init?.let {
            fieldSpecBuilder.initializer(buildCodeBlockFromExpression(it))
        }
        return fieldSpecBuilder.build()
    }

    private fun writeTestResource(source: Source, resourcePath: String, resourceName: String) {
        val dir = this.resourcesDirectory.resolve(resourcePath)
        if (!Files.exists(dir)) {
            Files.createDirectories(dir)
        }
        Files.newOutputStream(dir.resolve(resourceName)).use { outStream ->
            inputStream(source).use { inStream ->
                inStream.transferTo(outStream)
            }
        }
    }
}
