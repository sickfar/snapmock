package org.snapmock.generator

import org.snapmock.generator.data.Assertion
import org.snapmock.generator.data.Mock
import org.snapmock.generator.data.SnapMockTest
import org.snapmock.generator.framework.assertion.hamcrest.HamcrestAssertGenerator
import org.snapmock.generator.framework.mock.mockito.MockitoMockGenerator
import org.snapmock.generator.lang.common.Field
import org.snapmock.generator.lang.common.JvmAnnotation
import org.snapmock.generator.lang.java.JavaCodeGenerator
import org.snapmock.generator.lang.kotlin.KotlinCodeGenerator
import org.snapmock.snap.core.InvocationSnap
import org.snapmock.snap.core.Source
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

    fun generateMockingDependency(invocation: InvocationSnap, source: Source, depIndex: Int): Field

    fun generateMock(invocation: InvocationSnap, source: Source, depIndex: Int): Mock

}

interface AssertFrameworkGenerator {

    fun generateStatics(): List<String>

    fun generateTestClassAnnotations(): List<JvmAnnotation>

    fun generateTestMethodAttributes(): List<JvmAnnotation>

    fun generateAssertions(invocation: InvocationSnap, source: Source): Assertion

}
