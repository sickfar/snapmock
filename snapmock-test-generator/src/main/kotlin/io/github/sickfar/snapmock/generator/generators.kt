package io.github.sickfar.snapmock.generator

import io.github.sickfar.snapmock.core.InvocationSnap
import io.github.sickfar.snapmock.core.Source
import io.github.sickfar.snapmock.generator.data.Assertion
import io.github.sickfar.snapmock.generator.data.Mock
import io.github.sickfar.snapmock.generator.data.SnapMockTest
import io.github.sickfar.snapmock.generator.framework.assertion.hamcrest.HamcrestAssertGenerator
import io.github.sickfar.snapmock.generator.framework.mock.mockito.MockitoMockGenerator
import io.github.sickfar.snapmock.generator.lang.common.Field
import io.github.sickfar.snapmock.generator.lang.common.FieldRef
import io.github.sickfar.snapmock.generator.lang.common.JvmAnnotation
import io.github.sickfar.snapmock.generator.lang.java.JavaCodeGenerator
import io.github.sickfar.snapmock.generator.lang.kotlin.KotlinCodeGenerator
import java.nio.file.Path

fun assertGenerator(assertFramework: AssertFramework, testFramework: TestFramework) = when (assertFramework) {
    AssertFramework.HAMCREST -> HamcrestAssertGenerator(testFramework)
}

fun mockGenerator(mockFramework: MockFramework, testFramework: TestFramework) = when (mockFramework) {
    MockFramework.MOCKITO -> MockitoMockGenerator(testFramework)
}

fun codeGenerator(lang: Lang, mode: Mode, output: Path) = when (lang) {
    Lang.JAVA -> JavaCodeGenerator(mode, output)
    Lang.KOTLIN -> KotlinCodeGenerator(mode, output)
}

interface CodeGenerator {

    fun generate(className: String, methodName: String?, tests: List<SnapMockTest>): Path

}

interface MockFrameworkGenerator {

    fun generateStatics(): List<String>

    fun generateTestClassAnnotations(): List<JvmAnnotation>

    fun generateTestMethodAnnotations(): List<JvmAnnotation>

    fun generateSubject(invocation: InvocationSnap, source: Source): Field

    fun generateMockingDependencyDeclaration(source: Source, name: String, className: String): Field

    fun generateMockingDependency(invocation: InvocationSnap, source: Source, depIndex: Int): FieldRef

    fun generateMock(invocation: InvocationSnap, source: Source, depIndex: Int): Mock

}

interface AssertFrameworkGenerator {

    fun generateStatics(): List<String>

    fun generateTestClassAnnotations(): List<JvmAnnotation>

    fun generateTestMethodAttributes(): List<JvmAnnotation>

    fun generateAssertions(invocation: InvocationSnap, source: Source): Assertion

}
