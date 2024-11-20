package io.github.sickfar.snapmock.generator.lang.common

interface AnnotationExpression

data class ClassAnnotationExpression(
    val className: String
): AnnotationExpression

data class StringAnnotationExpression(
    val value: String
) : AnnotationExpression

data class JvmAnnotation(
    val className: String,
    val members: Map<String, AnnotationExpression> = mapOf()
)
