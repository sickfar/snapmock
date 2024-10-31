package org.snapmock.generator

import org.snapmock.generator.data.*
import org.snapmock.generator.framework.assertion.hamcrest.HamcrestAssertGenerator
import org.snapmock.generator.framework.mock.mockito.MockitoMockGenerator
import org.snapmock.generator.framework.test.junit.JUnitTestFrameworkGenerator
import org.snapmock.generator.lang.java.JavaCodeGenerator
import org.snapmock.generator.lang.kotlin.KotlinCodeGenerator
import org.snapmock.snap.core.InvocationSnap
import java.nio.file.Path

fun testGenerator(testFramework: TestFramework) = when (testFramework) {
    TestFramework.JUNIT -> JUnitTestFrameworkGenerator()
}

fun assertGenerator(assertFramework: AssertFramework) = when (assertFramework) {
    AssertFramework.HAMCREST -> HamcrestAssertGenerator()
}

fun mockGenerator(mockFramework: MockFramework) = when (mockFramework) {
    MockFramework.MOCKITO -> MockitoMockGenerator()
}

fun codeGenerator(lang: Lang) = when (lang) {
    Lang.JAVA -> JavaCodeGenerator()
    Lang.KOTLIN -> KotlinCodeGenerator()
}

interface CodeGenerator {

    fun generate(output: Path, forClass: String, snaps: List<SnapMockTest>): Path

}

interface TestFrameworkGenerator {

    fun generateTestClassAttributes(): TestClassAttributes?

    fun generateTestMethodAttributes(): TestMethodAttributes?

}

interface MockFrameworkGenerator {

    fun generateTestClassAttributes(): TestClassAttributes?

    fun generateTestMethodAttributes(): TestMethodAttributes?

    fun generateTestedElement(invocation: InvocationSnap): TestElement

    fun generateMockingDependency(invocation: InvocationSnap): TestElement

    fun generateMock(invocation: InvocationSnap): Mock

}

interface AssertFrameworkGenerator {

    fun generateTestClassAttributes(): TestClassAttributes?

    fun generateTestMethodAttributes(): TestMethodAttributes?

    fun generateAssertions(invocation: InvocationSnap): Assertion

}
