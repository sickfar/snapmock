package org.snapmock.generator.lang.java

import com.palantir.javapoet.ClassName
import com.palantir.javapoet.CodeBlock
import org.snapmock.generator.lang.common.*

fun buildCodeBlockFromExpression(expression: SyntaxElement): CodeBlock {
    return when (expression) {
        is NumericLiteral -> CodeBlock.of("${expression.value}")
        is StringLiteral -> CodeBlock.of("\"${expression.value}\"")
        is ClassRef -> CodeBlock.of("\$T.class", expression.name)
        is VariableDefinition -> buildVariableDefinition(expression)
        is StaticFieldRef -> buildStaticFieldRef(expression)
        is NamedRef -> CodeBlock.of(expression.name)
        is StaticMethod -> buildStaticMethodCall(expression)
        is InstanceMethod -> buildInstanceMethodCall(expression)
        is Lambda -> buildLambda(expression)
        else -> throw IllegalArgumentException("Unexpected expression: $expression")
    }
}

fun buildVariableDefinition(expression: VariableDefinition): CodeBlock {
    val cbBuilder = if (expression.final) {
        CodeBlock.builder().add("final ")
    } else {
        CodeBlock.builder()
    }
    cbBuilder.add("var \$L = ", expression.name)
    if (expression.init != null) {
        cbBuilder.add(buildCodeBlockFromExpression(expression.init))
    }
    return cbBuilder.build()
}

fun buildStaticFieldRef(expression: StaticFieldRef): CodeBlock =
    CodeBlock.of("\$T.\$L", ClassName.bestGuess(expression.typeName), expression.fieldName)

fun buildStaticMethodCall(expression: StaticMethod): CodeBlock =
    CodeBlock.builder()
        .addNamed(
            "\$className:T.\$methodName:L(", mapOf(
                "className" to ClassName.bestGuess(expression.typeName),
                "methodName" to expression.methodName
            )
        ).add(expression.arguments.stream().map { buildCodeBlockFromExpression(it) }
            .collect(CodeBlock.joining(", "))).add(")").build()

fun buildInstanceMethodCall(expression: InstanceMethod): CodeBlock {
    val codeBlockBuilder = CodeBlock.builder();
    if (expression.value is This) {
        codeBlockBuilder.add("${expression.methodName}(")
    } else {
        codeBlockBuilder
            .add(buildCodeBlockFromExpression(expression.value))
            .add(".${expression.methodName}(")
    }
    codeBlockBuilder
        .add(expression.arguments.stream().map { buildCodeBlockFromExpression(it) }.collect(CodeBlock.joining(", ")))
        .add(")")
    return codeBlockBuilder.build()
}

fun buildLambda(expression: Lambda): CodeBlock {
    val codeBlockBuilder = CodeBlock.builder();
    if (expression.parameters.size == 1) {
        codeBlockBuilder.add(expression.parameters.first()).add(" -> ")
    } else {
        codeBlockBuilder
            .add("(").add(expression.parameters.stream().map { CodeBlock.of(it) }.collect(CodeBlock.joining(", ")))
            .add(") -> ")
    }
    if (expression.body.size > 1) {
        codeBlockBuilder.beginControlFlow("")
        expression.body.forEach {
            codeBlockBuilder.addStatement(buildCodeBlockFromExpression(it))
        }
        codeBlockBuilder.endControlFlow()
    } else {
        codeBlockBuilder.add(buildCodeBlockFromExpression(expression.body.first()))
    }
    return codeBlockBuilder.build()
}
