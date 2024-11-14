package org.snapmock.generator.lang.java

import com.palantir.javapoet.ClassName
import com.palantir.javapoet.CodeBlock
import org.snapmock.generator.lang.common.*

fun buildCodeBlockFromExpression(expression: SyntaxElement): CodeBlock {
    return when (expression) {
        is NumericLiteral -> CodeBlock.of("${expression.value}")
        is StringLiteral -> CodeBlock.of("\"${expression.value}\"")
        is ClassRef -> CodeBlock.of("\$T.class", ClassName.bestGuess(expression.name))
        is VariableDefinition -> buildVariableDefinition(expression)
        is StaticFieldRef -> buildStaticFieldRef(expression)
        is InstanceFieldRef -> buildInstanceFieldRef(expression)
        is This -> buildThis(expression)
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
    if (expression.init != null) {
        cbBuilder.add("var \$L", expression.name)
        cbBuilder.add(" = ").add(buildCodeBlockFromExpression(expression.init))
    } else {
        val typeName = ClassName.bestGuess(expression.typeName).let {
            if (it.isBoxedPrimitive) {
                it.unbox()
            } else {
                it
            }
        }
        cbBuilder.add("\$T \$L", typeName, expression.name)
    }
    return cbBuilder.build()
}

fun buildStaticFieldRef(expression: StaticFieldRef): CodeBlock =
    CodeBlock.of("\$T.\$L", ClassName.bestGuess(expression.typeName), expression.fieldName)

fun buildInstanceFieldRef(expression: InstanceFieldRef): CodeBlock {
    val builder = CodeBlock.builder()
    val callee = buildCodeBlockFromExpression(expression.instance)
    if (!callee.isEmpty) {
        builder.add("\$L.", callee)
    }
    builder.add("\$L", expression.fieldName)
    return builder.build()
}

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
    val codeBlockBuilder = CodeBlock.builder()
    val callee = buildCodeBlockFromExpression(expression.instance)
    if (!callee.isEmpty) {
        codeBlockBuilder.add(callee).add(".")
    }
    codeBlockBuilder.add("${expression.methodName}(")
    codeBlockBuilder
        .add(expression.arguments.stream().map { buildCodeBlockFromExpression(it) }.collect(CodeBlock.joining(", ")))
        .add(")")
    return codeBlockBuilder.build()
}

fun buildLambda(expression: Lambda): CodeBlock {
    val codeBlockBuilder = CodeBlock.builder()
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

fun buildThis(expression: This): CodeBlock {
    if (expression.omitThis) {
        return CodeBlock.of("")
    } else {
        return CodeBlock.of("this")
    }
}
