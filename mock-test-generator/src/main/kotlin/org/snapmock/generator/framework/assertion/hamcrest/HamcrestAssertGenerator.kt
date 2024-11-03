package org.snapmock.generator.framework.assertion.hamcrest

import org.junit.jupiter.api.Assertions
import org.snapmock.generator.AssertFrameworkGenerator
import org.snapmock.generator.TestFramework
import org.snapmock.generator.data.Assertion
import org.snapmock.generator.data.ResultAssertion
import org.snapmock.generator.data.ThrowsAssertion
import org.snapmock.generator.lang.common.*
import org.snapmock.snap.core.InvocationSnap
import org.snapmock.snap.core.Source
import org.snapmock.snap.core.TestSupport

class HamcrestAssertGenerator(
    private val testFramework: TestFramework
) : AssertFrameworkGenerator {
    override fun generateStatics(): List<String> {
        return when (testFramework) {
            TestFramework.JUNIT -> listOf(Assertions::class.qualifiedName!!)
        }
    }

    override fun generateTestClassAnnotations(): List<JvmAnnotation> = listOf()

    override fun generateTestMethodAttributes(): List<JvmAnnotation> = listOf()

    override fun generateAssertions(invocation: InvocationSnap, source: Source): Assertion {
        return when (testFramework) {
            TestFramework.JUNIT -> generateJUnitAndHamcrestAssertions(invocation, source)
        }
    }

    private fun generateJUnitAndHamcrestAssertions(invocation: InvocationSnap, source: Source): Assertion {
        return if (invocation.exceptionType != null) {
            ThrowsAssertion(
                expectedExceptionClass = invocation.exceptionType!!,
                expectedExceptionMessage = invocation.exceptionMessage,
                statements = generateThrowsExpression(invocation)
            )
        } else {
            ResultAssertion(
                expected = invocation.result,
                statements = generateAssertResultExpression(invocation)
            )
        }
    }

    private fun generateThrowsExpression(invocation: InvocationSnap): List<SyntaxElement> {
        val exceptionType: String = invocation.exceptionType!!
        val exceptionMessage: String? = invocation.exceptionMessage
        val assertionBody = StaticMethod(
            typeName = Assertions::class.qualifiedName!!,
            methodName = "assertThrows",
            arguments = listOf(
                ClassRef(exceptionType),
                Lambda(
                    parameters = listOf(),
                    body = listOf(
                        InstanceMethod(
                            typeName = invocation.className,
                            value = FieldRef("subject"),
                            methodName = invocation.methodName,
                            arguments = List(invocation.arguments.size) { argIndex ->
                                StaticMethod(
                                    typeName = TestSupport::class.qualifiedName!!,
                                    methodName = "mainArg",
                                    arguments = listOf(
                                        FieldRef("source"),
                                        NumericLiteral(argIndex)
                                    )
                                )
                            }
                        )
                    )
                )
            )
        )
        if (exceptionMessage == null) {
            return listOf(
                assertionBody
            )
        } else {
            return listOf(
                VariableDefinition(
                    typeName = exceptionType,
                    name = "exception",
                    init = assertionBody
                ),
                StaticMethod(
                    typeName = Assertions::class.qualifiedName!!,
                    methodName = "assertEquals",
                    arguments = listOf(
                        StringLiteral(exceptionMessage),
                        InstanceFieldRef(
                            fieldName = "message",
                            instance = Variable(
                                typeName = exceptionType,
                                name = "exception",
                            )
                        )
                    )
                )
            )
        }
    }

    private fun generateAssertResultExpression(invocation: InvocationSnap): List<SyntaxElement> {
        val subjectCall = InstanceMethod(
            typeName = invocation.className,
            value = FieldRef("subject"),
            methodName = invocation.methodName,
            arguments = List(invocation.arguments.size) { argIndex ->
                StaticMethod(
                    typeName = TestSupport::class.qualifiedName!!,
                    methodName = "mainArg",
                    arguments = listOf(
                        FieldRef("source"),
                        NumericLiteral(argIndex)
                    )
                )
            }
        )
        return listOf(
            VariableDefinition(
                typeName = invocation.returnType,
                name = "result",
                init = subjectCall
            ),
            StaticMethod(
                typeName = Assertions::class.qualifiedName!!,
                methodName = "assertEquals",
                arguments = listOf(
                    StaticMethod(
                        typeName = TestSupport::class.qualifiedName!!,
                        methodName = "mainResult",
                        arguments = listOf(
                            FieldRef("source"),
                        )
                    ),
                    Variable(
                        typeName = invocation.returnType,
                        name = "result",
                    )
                )
            )
        )
    }
}
