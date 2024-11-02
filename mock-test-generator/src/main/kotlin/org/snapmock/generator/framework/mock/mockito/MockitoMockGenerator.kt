package org.snapmock.generator.framework.mock.mockito

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.stubbing.Stubber
import org.snapmock.generator.MockFrameworkGenerator
import org.snapmock.generator.TestFramework
import org.snapmock.generator.data.InvokeMock
import org.snapmock.generator.data.Mock
import org.snapmock.generator.data.ThrowMock
import org.snapmock.generator.lang.common.*
import org.snapmock.snap.core.InvocationSnap
import org.snapmock.snap.core.Source
import java.nio.file.Path

class MockitoMockGenerator(
    private val testFramework: TestFramework
) : MockFrameworkGenerator {

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

    override fun generateMockingDependency(invocation: InvocationSnap, source: Source, index: Int): Field {
        return Field(
            annotations = listOf(JvmAnnotation(org.mockito.Mock::class.qualifiedName!!)),
            typeName = invocation.className,
            name = invocation.className.substringAfterLast(".").replaceFirstChar { it.lowercase() },
            modifiers = setOf(),
            init = null
        )
    }

    override fun generateMock(invocation: InvocationSnap, source: Source, index: Int): Mock {
        //Mockito.doReturn(readResult(path, index)).when(dep).method(ArgumentMatcher.eq(readArg(path, 0, 0)))
        //Mockito.doThrow(readException(path, index)).when(dep).method(ArgumentMatcher.eq(readArg(path, 0, 0)))
        val dependency = generateMockingDependency(invocation, source, index)
        if (invocation.exceptionType != null) {
            val expression = InstanceMethod(
                typeName = invocation.className,
                value = InstanceMethod(
                    typeName = Stubber::class.qualifiedName!!,
                    value = StaticMethod(
                        typeName = Mockito::class.qualifiedName!!,
                        methodName = "doThrow",
                        arguments = listOf() //TODO readException(path, index)
                    ),
                    methodName = "when",
                    arguments = listOf(dependency)
                ),
                methodName = invocation.methodName,
                arguments = listOf() //TODO ArgumentMatcher.eq(readArg(path, 0 /*dep index*/, 0 /*arg index*/))
            )
            return ThrowMock(
                dependency = generateMockingDependency(invocation, source, index),
                method = invocation.methodName,
                arguments = invocation.arguments,
                source = Path.of("."),
                index = index,
                expression = expression,
                exceptionType = invocation.exceptionType!!,
                exceptionMessage = invocation.exceptionMessage,
            )
        } else {
            val expression = InstanceMethod(
                typeName = invocation.className,
                value = InstanceMethod(
                    typeName = Stubber::class.qualifiedName!!,
                    value = StaticMethod(
                        typeName = Mockito::class.qualifiedName!!,
                        methodName = "doReturn",
                        arguments = listOf() //TODO readResult(path, index)
                    ),
                    methodName = "when",
                    arguments = listOf(dependency)
                ),
                methodName = invocation.methodName,
                arguments = listOf()  //TODO ArgumentMatcher.eq(readArg(path, 0 /*dep index*/, 0 /*arg index*/))
            )
            return InvokeMock(
                dependency = generateMockingDependency(invocation, source, index),
                method = invocation.methodName,
                arguments = invocation.arguments,
                source = Path.of("."),
                index = index,
                result = invocation.result,
                expression = expression,
            )
        }
    }
}
