package org.snapmock.snap.spring

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.type.TypeFactory
import mu.KotlinLogging
import org.aopalliance.intercept.MethodInvocation
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.snapmock.core.*
import java.lang.reflect.Type
import java.util.*

private val log = KotlinLogging.logger {}

private val typeFactory = TypeFactory.defaultInstance()

internal fun snapDependencyInvocation(
    storage: InvocationStorage,
    dependencyType: Class<*>,
    invocation: MethodInvocation,
    result: Any
) {
    log.debug { "Dependency invocation: $invocation" }
    val method = invocation.method
    val methodParameters = Arrays.stream(method.genericParameterTypes)
        .map { type: Type? -> typeFactory.constructType(type) }
        .map { obj: JavaType -> obj.toCanonical() }
        .toList()
    val methodReturnType = typeFactory.constructType(method.genericReturnType)
    val snap = InvocationSnap(
        className = dependencyType.name,
        methodName = method.name,
        parameterTypes = methodParameters,
        returnType = methodReturnType.toCanonical(),
        arguments = listOf(*invocation.arguments),
        result = result,
        argumentTypes = null,
        exceptionType = null,
        exceptionMessage = null,
    )
    storage.record(snap)
}

internal fun snapFactoryInvocation(
    storage: InvocationStorage,
    dependencyType: Class<*>,
    invocation: MethodInvocation
) {
    log.debug { "Dependency factory invocation: $invocation" }
    val method = invocation.method
    val methodParameters = Arrays.stream(method.genericParameterTypes)
        .map { type: Type? -> typeFactory.constructType(type) }
        .map { obj: JavaType -> obj.toCanonical() }
        .toList()
    val methodReturnType = typeFactory.constructType(method.genericReturnType)
    val snap = FactoryInvocationSnap(
        className = dependencyType.name,
        methodName = method.name,
        parameterTypes = methodParameters,
        returnType = methodReturnType.toCanonical(),
        arguments = listOf(*invocation.arguments),
        argumentTypes = null,
        exceptionType = null,
        exceptionMessage = null,
    )
    storage.record(snap)
}

internal fun snapDependencyInvocationException(
    storage: InvocationStorage,
    dependencyType: Class<*>,
    invocation: MethodInvocation,
    exception: Throwable
) {
    log.debug { "Dependency invocation exception: $invocation, ${exception.message}" }
    val method = invocation.method
    val methodParameters = Arrays.stream(method.genericParameterTypes)
        .map { type: Type? -> typeFactory.constructType(type) }
        .map { obj: JavaType -> obj.toCanonical() }
        .toList()
    val methodReturnType = typeFactory.constructType(method.genericReturnType)
    val snap = InvocationSnap(
        className = dependencyType.name,
        methodName = method.name,
        parameterTypes = methodParameters,
        returnType = methodReturnType.toCanonical(),
        arguments = listOf(*invocation.arguments),
        exceptionType = exception.javaClass.name,
        exceptionMessage = exception.message,
        argumentTypes = null,
        result = null,
    )
    storage.record(snap)
}

internal fun snapFactoryInvocationException(
    storage: InvocationStorage,
    dependencyType: Class<*>,
    invocation: MethodInvocation,
    exception: Throwable
) {
    log.debug { "Dependency factory invocation exception: $invocation, ${exception.message}" }
    val method = invocation.method
    val methodParameters = Arrays.stream(method.genericParameterTypes)
        .map { type: Type? -> typeFactory.constructType(type) }
        .map { obj: JavaType -> obj.toCanonical() }
        .toList()
    val methodReturnType = typeFactory.constructType(method.genericReturnType)
    val snap = FactoryInvocationSnap(
        className = dependencyType.name,
        methodName = method.name,
        parameterTypes = methodParameters,
        returnType = methodReturnType.toCanonical(),
        arguments = listOf(*invocation.arguments),
        exceptionType = exception.javaClass.name,
        exceptionMessage = exception.message,
        argumentTypes = null
    )
    storage.record(snap)
}

internal fun snapInvocation(
    storage: InvocationStorage,
    writer: SnapWriter,
    joinPoint: ProceedingJoinPoint,
    result: Any
) {
    log.debug { "Snapping invocation" }
    val signature = joinPoint.signature as MethodSignature
    val method = signature.method
    val args = joinPoint.args
    val methodParameters = Arrays.stream(method.genericParameterTypes)
        .map { type: Type? -> typeFactory.constructType(type) }
        .map { obj: JavaType -> obj.toCanonical() }
        .toList()
    val className = joinPoint.signature.declaringTypeName
    val methodName = method.name
    val returnType = typeFactory.constructType(method.genericReturnType)
    val main = InvocationSnap(
        className = className,
        methodName = methodName,
        parameterTypes = methodParameters,
        returnType = returnType.toCanonical(),
        arguments = listOf(*args),
        result = result,
        argumentTypes = null,
        exceptionType = null,
        exceptionMessage = null,
    )
    val snap = SnapData(
        main = main,
        dependencies = storage.getDependencyInvocations(),
        factories = storage.getFactoryInvocations()
    )
    writer.write(snap)
    storage.reset()
}

internal fun snapInvocationException(
    storage: InvocationStorage,
    writer: SnapWriter,
    joinPoint: ProceedingJoinPoint,
    throwable: Throwable
) {
    log.debug { "Snapping invocation exception" }
    val signature = joinPoint.signature as MethodSignature
    val method = signature.method
    val args = joinPoint.args
    val methodParameters = Arrays.stream(method.genericParameterTypes)
        .map { type: Type? -> typeFactory.constructType(type) }
        .map { obj: JavaType -> obj.toCanonical() }
        .toList()
    val className = joinPoint.signature.declaringTypeName
    val methodName = method.name
    val returnType = typeFactory.constructType(method.genericReturnType)
    val main = InvocationSnap(
        className = className,
        methodName = methodName,
        parameterTypes = methodParameters,
        returnType = returnType.toCanonical(),
        arguments = listOf(*args),
        exceptionType = throwable.javaClass.name,
        exceptionMessage = throwable.message,
        argumentTypes = null,
        result = null,
    )
    val snap = SnapData(
        main = main,
        dependencies = storage.getDependencyInvocations(),
        factories = storage.getFactoryInvocations()
    )
    writer.write(snap)
    storage.reset()
}
