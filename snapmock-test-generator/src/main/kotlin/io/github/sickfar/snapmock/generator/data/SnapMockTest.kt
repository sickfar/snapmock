package io.github.sickfar.snapmock.generator.data

import io.github.sickfar.snapmock.core.Source
import io.github.sickfar.snapmock.generator.lang.common.Field
import io.github.sickfar.snapmock.generator.lang.common.FieldRef
import io.github.sickfar.snapmock.generator.lang.common.JvmAnnotation
import io.github.sickfar.snapmock.generator.lang.common.SyntaxElement
import java.nio.file.Path

interface Mock {
    val expression: SyntaxElement
}

data class InvokeMock(
    val dependency: FieldRef,
    val method: String,
    val arguments: List<Any?>,
    var result: Any?,
    val source: Path,
    val index: Int,
    override val expression: SyntaxElement
): Mock

data class ThrowMock(
    val dependency: FieldRef,
    val method: String,
    val arguments: List<Any?>,
    val exceptionType: String,
    val exceptionMessage: String?,
    val source: Path,
    val index: Int,
    override val expression: SyntaxElement
): Mock

interface Assertion {
    val statements: List<SyntaxElement>
}

data class ResultAssertion (
    val expected: Any?,
    override val statements: List<SyntaxElement>
): Assertion

data class ThrowsAssertion(
    val expectedExceptionClass: String,
    val expectedExceptionMessage: String?,
    override val statements: List<SyntaxElement>
): Assertion

data class SnapMockTest(
    val source: Source,
    val statics: List<String>,
    val testClassAnnotations: List<JvmAnnotation>,
    val testMethodAnnotations: List<JvmAnnotation>,
    val subject: Field,
    val subjectMethod: String,
    val dependencies: Set<Field>,
    val mocks: List<Mock>,
    val assertion: Assertion,
)
