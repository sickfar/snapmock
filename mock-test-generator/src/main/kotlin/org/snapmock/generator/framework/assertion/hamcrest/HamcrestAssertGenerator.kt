package org.snapmock.generator.framework.assertion.hamcrest

import org.snapmock.generator.AssertFrameworkGenerator
import org.snapmock.generator.TestFramework
import org.snapmock.generator.data.Assertion
import org.snapmock.generator.lang.common.JvmAnnotation
import org.snapmock.snap.core.InvocationSnap
import org.snapmock.snap.core.Source

class HamcrestAssertGenerator(
    private val testFramework: TestFramework
): AssertFrameworkGenerator {
    override fun generateTestClassAnnotations(): List<JvmAnnotation> = listOf()

    override fun generateTestMethodAttributes(): List<JvmAnnotation> = listOf()

    override fun generateAssertions(invocation: InvocationSnap, source: Source): List<Assertion> {
        return when (testFramework) {
            TestFramework.JUNIT -> generateJUnitAndHamcrestAssertions(invocation, source)
        }
    }

    private fun generateJUnitAndHamcrestAssertions(invocation: InvocationSnap, source: Source): List<Assertion> {
        if (invocation.exceptionType != null) {

        } else {

        }
        TODO("Not yet implemented")
    }
}
