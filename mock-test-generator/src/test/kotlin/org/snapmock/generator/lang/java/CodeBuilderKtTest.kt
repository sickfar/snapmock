package org.snapmock.generator.lang.java

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.stubbing.Stubber
import org.snapmock.generator.lang.common.FieldRef
import org.snapmock.generator.lang.common.InstanceFieldRef
import org.snapmock.generator.lang.common.InstanceMethod
import org.snapmock.generator.lang.common.Lambda
import org.snapmock.generator.lang.common.NumericLiteral
import org.snapmock.generator.lang.common.Parameter
import org.snapmock.generator.lang.common.StaticFieldRef
import org.snapmock.generator.lang.common.StaticMethod
import org.snapmock.generator.lang.common.StringLiteral
import org.snapmock.generator.lang.common.This
import org.snapmock.generator.lang.common.Variable
import org.snapmock.generator.lang.common.VariableDefinition

class CodeBuilderKtTest {

    @Test
    fun testBuildStaticFieldRef() {
        val expression = StaticFieldRef(
            fieldName = "out",
            typeName = System::class.qualifiedName!!
        )
        val code = buildStaticFieldRef(expression)
        assertEquals("java.lang.System.out", code.toString())
    }

    @Test
    fun testInstanceFieldRef() {
        val expression = InstanceFieldRef(
            fieldName = "field",
            instance = This()
        )
        val code = buildInstanceFieldRef(expression)
        assertEquals("this.field", code.toString())
    }

    @Test
    fun testInstanceFieldRef_ThisOmit() {
        val expression = InstanceFieldRef(
            fieldName = "field",
            instance = This(true)
        )
        val code = buildInstanceFieldRef(expression)
        assertEquals("field", code.toString())
    }

    @Test
    fun testBuildStaticMethodCall_SimpleVariables() {
        val expression = StaticMethod(
            typeName = Mockito::class.qualifiedName!!,
            methodName = "doReturn",
            arguments = listOf(Variable("varName", "int"))
        )
        val code = buildStaticMethodCall(expression)
        assertEquals("org.mockito.Mockito.doReturn(varName)", code.toString())
    }

    @Test
    fun testBuildStaticMethodCall_Fields() {
        val expression = StaticMethod(
            typeName = Mockito::class.qualifiedName!!,
            methodName = "doReturn",
            arguments = listOf(FieldRef("fieldName"))
        )
        val code = buildStaticMethodCall(expression)
        assertEquals("org.mockito.Mockito.doReturn(fieldName)", code.toString())
    }

    @Test
    fun testBuildStaticMethodCall_Params() {
        val expression = StaticMethod(
            typeName = Mockito::class.qualifiedName!!,
            methodName = "doReturn",
            arguments = listOf(Parameter("paramName", "int"))
        )
        val code = buildStaticMethodCall(expression)
        assertEquals("org.mockito.Mockito.doReturn(paramName)", code.toString())
    }

    @Test
    fun testBuildStaticMethodCall_TwoParams() {
        val expression = StaticMethod(
            typeName = Mockito::class.qualifiedName!!,
            methodName = "doReturn",
            arguments = listOf(Parameter("paramName1", "int"), Parameter("paramName2", "int"))
        )
        val code = buildStaticMethodCall(expression)
        assertEquals("org.mockito.Mockito.doReturn(paramName1, paramName2)", code.toString())
    }

    @Test
    fun testBuildStaticMethodCall_OtherMethodInvocation() {
        val expression = StaticMethod(
            typeName = Mockito::class.qualifiedName!!,
            methodName = "doReturn",
            arguments = listOf(
                StaticMethod(
                    typeName = System::class.qualifiedName!!,
                    methodName = "currentTimeMillis",
                    arguments = listOf()
                )
            )
        )
        val code = buildStaticMethodCall(expression)
        assertEquals("org.mockito.Mockito.doReturn(java.lang.System.currentTimeMillis())", code.toString())
    }

    @Test
    fun testBuildInstanceMethodCall_This() {
        val expression = InstanceMethod(
            typeName = javaClass.name,
            instance = This(true),
            methodName = "someMethod",
            arguments = listOf()
        )
        val code = buildInstanceMethodCall(expression)
        assertEquals("someMethod()", code.toString())
    }

    @Test
    fun testBuildInstanceMethodCall_StaticField() {
        val expression = InstanceMethod(
            typeName = javaClass.name,
            instance = StaticFieldRef(
                fieldName = "out",
                typeName = System::class.qualifiedName!!,
            ),
            methodName = "println",
            arguments = listOf(StringLiteral("Hello World!"))
        )
        val code = buildInstanceMethodCall(expression)
        assertEquals("java.lang.System.out.println(\"Hello World!\")", code.toString())
    }

    @Test
    fun testBuildLambda_NoParameters() {
        val expression = Lambda(
            parameters = listOf(),
            body = listOf(
                InstanceMethod(
                    typeName = javaClass.name,
                    instance = StaticFieldRef(
                        fieldName = "out",
                        typeName = System::class.qualifiedName!!,
                    ),
                    methodName = "println",
                    arguments = listOf(StringLiteral("Hello World!"))
                )
            )
        )
        val code = buildLambda(expression)
        assertEquals("() -> java.lang.System.out.println(\"Hello World!\")", code.toString())
    }

    @Test
    fun testBuildLambda_SingleParameter() {
        val expression = Lambda(
            parameters = listOf("input"),
            body = listOf(
                InstanceMethod(
                    typeName = javaClass.name,
                    instance = StaticFieldRef(
                        fieldName = "out",
                        typeName = System::class.qualifiedName!!,
                    ),
                    methodName = "println",
                    arguments = listOf(Parameter("input", "java.lang.String"))
                )
            )
        )
        val code = buildLambda(expression)
        assertEquals("input -> java.lang.System.out.println(input)", code.toString())
    }

    @Test
    fun testBuildLambda_MultipleParameters() {
        val expression = Lambda(
            parameters = listOf("input", "input1"),
            body = listOf(
                InstanceMethod(
                    typeName = javaClass.name,
                    instance = StaticFieldRef(
                        fieldName = "out",
                        typeName = System::class.qualifiedName!!,
                    ),
                    methodName = "println",
                    arguments = listOf(Parameter("input", "java.lang.String"))
                )
            )
        )
        val code = buildLambda(expression)
        assertEquals("(input, input1) -> java.lang.System.out.println(input)", code.toString())
    }

    @Test
    fun testBuildLambda_MultipleParametersMultipleStatements() {
        val expression = Lambda(
            parameters = listOf("input", "input1"),
            body = listOf(
                InstanceMethod(
                    typeName = javaClass.name,
                    instance = StaticFieldRef(
                        fieldName = "out",
                        typeName = System::class.qualifiedName!!,
                    ),
                    methodName = "println",
                    arguments = listOf(Parameter("input", "java.lang.String"))
                ),
                InstanceMethod(
                    typeName = javaClass.name,
                    instance = StaticFieldRef(
                        fieldName = "out",
                        typeName = System::class.qualifiedName!!,
                    ),
                    methodName = "println",
                    arguments = listOf(Parameter("input1", "java.lang.String"))
                )
            )
        )
        val code = buildLambda(expression)
        assertEquals(
            """(input, input1) ->  {
            |  java.lang.System.out.println(input);
            |  java.lang.System.out.println(input1);
            |}
            |""".trimMargin(), code.toString()
        )
    }

    @Test
    fun testBuildVariableDefinitionFinal_NoInit() {
        val expression = VariableDefinition(
            typeName = Integer::class.java.name,
            name = "test",
            final = true,
            init = null
        )
        val code = buildVariableDefinition(expression)
        assertEquals("final int test", code.toString())
    }

    @Test
    fun testBuildVariableDefinitionFinal_WithInit() {
        val expression = VariableDefinition(
            typeName = "int",
            name = "test",
            final = true,
            init = NumericLiteral(5)
        )
        val code = buildVariableDefinition(expression)
        assertEquals("final var test = 5", code.toString())
    }

    @Test
    fun testBuildVariableDefinition_NoInit() {
        val expression = VariableDefinition(
            typeName = String::class.java.name,
            name = "test",
            final = false,
            init = null
        )
        val code = buildVariableDefinition(expression)
        assertEquals("java.lang.String test", code.toString())
    }

    @Test
    fun testBuildVariableDefinition_WithInit() {
        val expression = VariableDefinition(
            typeName = String::class.java.name,
            name = "test",
            final = false,
            init = StringLiteral("Hello World!"),
        )
        val code = buildVariableDefinition(expression)
        assertEquals("var test = \"Hello World!\"", code.toString())
    }

    @Test
    fun testBuildCodeBlockFromSimpleExpression() {
        val expression = InstanceMethod(
            typeName = "org.snapmock.snap.spring.simple.app.HelloService",
            instance = InstanceMethod(
                typeName = Stubber::class.qualifiedName!!,
                instance = StaticMethod(
                    typeName = Mockito::class.qualifiedName!!,
                    methodName = "doThrow",
                    arguments = listOf(Variable("variable", "int"))
                ),
                methodName = "when",
                arguments = listOf(FieldRef(name = "helloService"))
            ),
            methodName = "get",
            arguments = listOf(
                StaticMethod(
                    typeName = ArgumentMatchers::class.qualifiedName!!,
                    methodName = "eq",
                    arguments = listOf(StringLiteral("Hello"))
                )
            )
        )
        val code = buildCodeBlockFromExpression(expression)
        assertEquals(
            "org.mockito.Mockito.doThrow(variable).when(helloService).get(org.mockito.ArgumentMatchers.eq(\"Hello\"))",
            code.toString()
        )
    }
}
