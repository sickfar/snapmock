package org.snapmock.generator.lang.java

import com.palantir.javapoet.*
import org.mockito.Mockito
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
            test.mocks.forEach { mock ->
                testMethodBuilder.addStatement(buildCodeBlockFromExpression(mock.expression))
            }
            test.assertions.forEach { assertion ->
                testMethodBuilder.addStatement(buildCodeBlockFromExpression(assertion.expression))
            }
            testClassBuilder.addMethod(testMethodBuilder.build())
        }
        val javaFile = JavaFile.builder(packageName, testClassBuilder.build())
            .addStaticImport(Mockito::class.java, "*")
            .build()
        return javaFile.writeToPath(codeDirectory)
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
}
