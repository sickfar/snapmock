package org.snapmock.generator.lang.common

interface SyntaxElement

interface Invokable: SyntaxElement

data class StaticMethod(
    val typeName: String,
    val methodName: String,
    val arguments: List<SyntaxElement>,
): Invokable

data class InstanceMethod(
    val typeName: String,
    val value: SyntaxElement,
    val methodName: String,
    val arguments: List<SyntaxElement>,
): Invokable

data class Lambda(
    val parameters: List<String>,
    val body: List<SyntaxElement>
): Invokable

data class StringExpr(
    val value: String
): SyntaxElement

interface NamedRef: SyntaxElement {
    val name: String
}

data class Field(
    override val name: String,
    val typeName: String,
    val annotations: List<JvmAnnotation>,
    val modifiers: Set<String>,
    val init: SyntaxElement?
): NamedRef

data class VariableDefinition(
    val typeName: String,
    override val name: String,
    val init: SyntaxElement?
): NamedRef

data class Variable(
    override val name: String,
    val typeName: String,
): NamedRef

data class Parameter(
    override val name: String,
    val typeName: String,
): NamedRef

data class StaticFieldReference(
    val fieldName: String,
    val typeName: String,
): NamedRef {
    override val name: String = "$typeName.$fieldName"
}

class This: NamedRef {
    override val name: String = "this"
}


