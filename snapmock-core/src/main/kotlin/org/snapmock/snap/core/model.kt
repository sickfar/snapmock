package org.snapmock.snap.core

data class InvocationSnap(
    val className: String,
    val methodName: String,
    val parameterTypes: List<String>,
    val arguments: List<Any>,
    val argumentTypes: List<String>?,
    val returnType: String,
    val result: Any?,
    val exceptionType: String?,
    val exceptionMessage: String?,
)

data class SnapData(
    val main: InvocationSnap,
    val dependencies: Collection<InvocationSnap>
)
