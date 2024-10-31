package org.snapmock.generator.data

import com.sun.tools.classfile.Dependency
import java.nio.file.Path

data class JvmAnnotation(
    val className: String,
    val members: List<String>
)

data class TestClassAttributes(
    val annotations: List<JvmAnnotation>
)

data class TestMethodAttributes(
    val annotations: List<JvmAnnotation>,
    val name: String,
)

data class TestElement(
    val annotations: List<JvmAnnotation>,
    val className: String,
    val name: String,
    val initExpression: String
)

data class Mock(
    val dependency: Dependency,
    val method: String,
    val arguments: List<Any>,
    val source: Path,
    val index: Int
)

interface Assertion

data class ResultAssertion (
    val expected: Any
): Assertion

data class ThrowsAssertion(
    val expectedExceptionClass: String,
    val expectedExceptionMessage: String,
): Assertion

data class SnapMockTest(
    val testClassAttributes: TestClassAttributes,
    val testMethodAttributes: TestMethodAttributes,
    val testedClass: TestElement,
    val dependencies: List<TestElement>,
    val mocks: List<Mock>,
    val assertions: List<Assertion>,
)
