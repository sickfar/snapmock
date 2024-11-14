package org.snapmock.generator.framework.mock.mockito

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.stubbing.Stubber
import org.snapmock.core.InvocationSnap
import org.snapmock.core.Source
import org.snapmock.core.TestSupport
import org.snapmock.generator.MockFrameworkGenerator
import org.snapmock.generator.TestFramework
import org.snapmock.generator.data.InvokeMock
import org.snapmock.generator.data.Mock
import org.snapmock.generator.data.ThrowMock
import org.snapmock.generator.lang.common.*
import org.snapmock.mock.mockito.MockitoTestSupport
import java.nio.file.Path

class MockitoMockGenerator(
    private val testFramework: TestFramework
) : MockFrameworkGenerator {

    override fun generateStatics(): List<String> = listOf(
        MockitoTestSupport::class.qualifiedName!!,
        ArgumentMatchers::class.qualifiedName!!,
    )

    override fun generateTestClassAnnotations(): List<JvmAnnotation> {
        return when (testFramework) {
            TestFramework.JUNIT -> listOf(
                JvmAnnotation(
                    ExtendWith::class.qualifiedName!!,
                    members = mapOf("value" to ClassAnnotationExpression(MockitoExtension::class.qualifiedName!!))
                )
            )
        }
    }

    override fun generateTestMethodAnnotations(): List<JvmAnnotation> {
        return when (testFramework) {
            TestFramework.JUNIT -> listOf(
                JvmAnnotation(
                    Test::class.qualifiedName!!,
                    members = mapOf()
                )
            )
        }
    }

    override fun generateSubject(invocation: InvocationSnap, source: Source): Field {
        return Field(
            annotations = listOf(JvmAnnotation(InjectMocks::class.qualifiedName!!)),
            typeName = invocation.className,
            name = "subject",
            modifiers = setOf(),
            init = null
        )
    }

    override fun generateMockingDependency(invocation: InvocationSnap, source: Source, depIndex: Int): FieldRef {
        return FieldRef(
            name = invocation.className.substringAfterLast(".").replaceFirstChar { it.lowercase() },
        )
    }

    override fun generateMockingDependencyDeclaration(source: Source, name: String, className: String): Field {
        return Field(
            annotations = listOf(JvmAnnotation(org.mockito.Mock::class.qualifiedName!!)),
            typeName = className,
            name = className.substringAfterLast(".").replaceFirstChar { it.lowercase() },
            modifiers = setOf(),
            init = null
        )
    }

    override fun generateMock(invocation: InvocationSnap, source: Source, depIndex: Int): Mock {
        //Mockito.doReturn(readResult(path, index)).when(dep).method(ArgumentMatcher.eq(readArg(path, 0, 0)))
        //Mockito.doThrow(readException(path, index)).when(dep).method(ArgumentMatcher.eq(readArg(path, 0, 0)))
        val dependency = generateMockingDependency(invocation, source, depIndex)
        val arguments = List(invocation.arguments.size) { argIndex ->
            StaticMethod(
                typeName = ArgumentMatchers::class.qualifiedName!!,
                methodName = "eq",
                arguments = listOf(
                    StaticMethod(
                        typeName = TestSupport::class.qualifiedName!!,
                        methodName = "depArg",
                        arguments = listOf(
                            FieldRef("source"),
                            NumericLiteral(depIndex),
                            NumericLiteral(argIndex),
                        )
                    )
                )
            )
        }
        if (invocation.exceptionType != null) {
            val expression = InstanceMethod(
                typeName = invocation.className,
                instance = InstanceMethod(
                    typeName = Stubber::class.qualifiedName!!,
                    instance = StaticMethod(
                        typeName = Mockito::class.qualifiedName!!,
                        methodName = "doThrow",
                        arguments = listOf(
                            StaticMethod(
                                typeName = MockitoTestSupport::class.qualifiedName!!,
                                methodName = "depThr",
                                arguments = listOf(
                                    FieldRef("source"),
                                    NumericLiteral(depIndex)
                                )
                            )
                        )
                    ),
                    methodName = "when",
                    arguments = listOf(dependency)
                ),
                methodName = invocation.methodName,
                arguments = arguments
            )
            return ThrowMock(
                dependency = generateMockingDependency(invocation, source, depIndex),
                method = invocation.methodName,
                arguments = invocation.arguments,
                source = Path.of("."),
                index = depIndex,
                expression = expression,
                exceptionType = invocation.exceptionType!!,
                exceptionMessage = invocation.exceptionMessage,
            )
        } else {
            val expression = InstanceMethod(
                typeName = invocation.className,
                instance = InstanceMethod(
                    typeName = Stubber::class.qualifiedName!!,
                    instance = StaticMethod(
                        typeName = Mockito::class.qualifiedName!!,
                        methodName = "doReturn",
                        arguments = listOf(
                            StaticMethod(
                                typeName = TestSupport::class.qualifiedName!!,
                                methodName = "depResult",
                                arguments = listOf(
                                    FieldRef("source"),
                                    NumericLiteral(depIndex),
                                )
                            )
                        )
                    ),
                    methodName = "when",
                    arguments = listOf(dependency)
                ),
                methodName = invocation.methodName,
                arguments = arguments
            )
            return InvokeMock(
                dependency = generateMockingDependency(invocation, source, depIndex),
                method = invocation.methodName,
                arguments = invocation.arguments,
                source = Path.of("."),
                index = depIndex,
                result = invocation.result,
                expression = expression,
            )
        }
    }
}
