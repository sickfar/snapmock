package org.snapmock.core

/**
 * Representation of snapshot taken from a method invocation
 *
 * @since 1.0.0
 * @author Roman Aksenenko
 */
data class InvocationSnap(
    val className: String,
    val methodName: String,
    val parameterTypes: List<String>,
    val arguments: List<Any>,
    val argumentTypes: List<String?>?,
    val returnType: String,
    val result: Any?,
    val exceptionType: String?,
    val exceptionMessage: String?,
)

/**
 * Representation of a factory bean method invocation recording
 *
 * @since 1.0.0
 * @author Roman Aksenenko
 */
data class FactoryInvocationSnap(
    val className: String,
    val methodName: String,
    val parameterTypes: List<String>,
    val arguments: List<Any>,
    val argumentTypes: List<String>?,
    val returnType: String,
    val exceptionType: String?,
    val exceptionMessage: String?,
)

/**
 * Representation of a full snapshot, taken from a main method annotated with [Snap]
 * @param main Snapshot of an invocation of method annotated with [Snap]
 * or a public method of class annotated with [Snap]
 * @param dependencies Snapshots of invocation of dependencies (members) of a class annotated with [Snap]
 * or a class which contains method annotated with [Snap]
 * @param factories Recordings of invocations of dependencies (members) of a class annotated with [Snap]
 * or a class which contains method annotated with [Snap] which are factory beans
 *
 * @since 1.0.0
 * @author Roman Aksenenko
 */
data class SnapData(
    /**
     * Snapshot of an invocation of method annotated with [Snap]
     * or a public method of class annotated with [Snap]
     */
    val main: InvocationSnap,
    /**
     * Snapshots of invocation of dependencies (members) of a class annotated with [Snap]
     * or a class which contains method annotated with [Snap]
     */
    val dependencies: List<InvocationSnap>,
    /**
     * Recordings of invocations of dependencies (members) of a class annotated with [Snap]
     * or a class which contains method annotated with [Snap] which are factory beans
     */
    val factories: List<FactoryInvocationSnap>
)
