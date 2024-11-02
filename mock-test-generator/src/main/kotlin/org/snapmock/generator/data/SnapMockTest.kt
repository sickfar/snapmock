package org.snapmock.generator.data

import org.snapmock.generator.lang.common.Field
import org.snapmock.generator.lang.common.JvmAnnotation
import org.snapmock.generator.lang.common.SyntaxElement
import java.nio.file.Path

interface Mock {
    val expression: SyntaxElement
}

data class InvokeMock(
    val dependency: Field,
    val method: String,
    val arguments: List<Any>,
    var result: Any?,
    val source: Path,
    val index: Int,
    override val expression: SyntaxElement
): Mock

data class ThrowMock(
    val dependency: Field,
    val method: String,
    val arguments: List<Any>,
    val exceptionType: String,
    val exceptionMessage: String?,
    val source: Path,
    val index: Int,
    override val expression: SyntaxElement
): Mock

interface Assertion {
    val expression: SyntaxElement
}

data class ResultAssertion (
    val expected: Any,
    override val expression: SyntaxElement
): Assertion

data class ThrowsAssertion(
    val expectedExceptionClass: String,
    val expectedExceptionMessage: String,
    override val expression: SyntaxElement
): Assertion

data class SnapMockTest(
    val testClassAnnotations: List<JvmAnnotation>,
    val testMethodAnnotations: List<JvmAnnotation>,
    val subject: Field,
    val subjectMethod: String,
    val dependencies: Set<Field>,
    val mocks: List<Mock>,
    val assertions: List<Assertion>,
)
