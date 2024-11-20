package io.github.sickfar.snapmock.generator.framework.assertion.hamcrest

import io.github.sickfar.snapmock.core.InvocationSnap
import io.github.sickfar.snapmock.core.Source
import io.github.sickfar.snapmock.core.TestSupport
import io.github.sickfar.snapmock.generator.AssertFrameworkGenerator
import io.github.sickfar.snapmock.generator.TestFramework
import io.github.sickfar.snapmock.generator.data.Assertion
import io.github.sickfar.snapmock.generator.data.ResultAssertion
import io.github.sickfar.snapmock.generator.data.ThrowsAssertion
import io.github.sickfar.snapmock.generator.lang.common.ClassRef
import io.github.sickfar.snapmock.generator.lang.common.FieldRef
import io.github.sickfar.snapmock.generator.lang.common.InstanceMethod
import io.github.sickfar.snapmock.generator.lang.common.JvmAnnotation
import io.github.sickfar.snapmock.generator.lang.common.Lambda
import io.github.sickfar.snapmock.generator.lang.common.NumericLiteral
import io.github.sickfar.snapmock.generator.lang.common.StaticMethod
import io.github.sickfar.snapmock.generator.lang.common.StringLiteral
import io.github.sickfar.snapmock.generator.lang.common.SyntaxElement
import io.github.sickfar.snapmock.generator.lang.common.Variable
import io.github.sickfar.snapmock.generator.lang.common.VariableDefinition
import org.junit.jupiter.api.Assertions

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
                            instance = FieldRef("subject"),
                            methodName = invocation.methodName,
                            arguments = List(invocation.arguments.size) { argIndex ->
                                StaticMethod(
                                    typeName = TestSupport::class.qualifiedName!!,
                                    methodName = "subjArg",
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
                        InstanceMethod(
                            typeName = exceptionType,
                            instance = Variable(
                                typeName = exceptionType,
                                name = "exception",
                            ),
                            methodName = "getMessage",
                            arguments = listOf()
                        )
                    )
                )
            )
        }
    }

    private fun generateAssertResultExpression(invocation: InvocationSnap): List<SyntaxElement> {
        val subjectCall = InstanceMethod(
            typeName = invocation.className,
            instance = FieldRef("subject"),
            methodName = invocation.methodName,
            arguments = List(invocation.arguments.size) { argIndex ->
                StaticMethod(
                    typeName = TestSupport::class.qualifiedName!!,
                    methodName = "subjArg",
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
                        methodName = "subjResult",
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
