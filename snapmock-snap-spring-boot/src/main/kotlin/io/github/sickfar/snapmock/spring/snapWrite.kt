package io.github.sickfar.snapmock.spring

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.type.TypeFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.sickfar.snapmock.core.FactoryInvocationSnap
import io.github.sickfar.snapmock.core.InvocationSnap
import io.github.sickfar.snapmock.core.InvocationStorage
import io.github.sickfar.snapmock.core.SnapData
import io.github.sickfar.snapmock.core.SnapWriter
import org.aopalliance.intercept.MethodInvocation
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.aop.framework.AopProxyUtils
import java.lang.reflect.Type
import java.util.*

private val log = KotlinLogging.logger {}

private val typeFactory = TypeFactory.defaultInstance()

internal fun snapDependencyInvocation(
    storage: InvocationStorage,
    dependencyType: Class<*>,
    invocation: MethodInvocation,
    result: Any?
) {
    log.debug { "Dependency invocation: $invocation" }
    val method = invocation.method
    val methodParameters = Arrays.stream(method.genericParameterTypes)
        .map { type: Type? -> typeFactory.constructType(type) }
        .map { obj: JavaType -> obj.toCanonical() }
        .toList()
    // actual method return type
    val methodReturnType = typeFactory.constructType(method.genericReturnType)
    val snap = InvocationSnap(
        className = dependencyType.name,
        methodName = method.name,
        parameterTypes = methodParameters,
        argumentTypes = invocation.arguments.map { getCanonicalTypeFromObject(it) },
        arguments = listOf(*invocation.arguments),
        returnType = methodReturnType.toCanonical(),
        resultType = getCanonicalTypeFromObject(result),
        result = result,
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
        argumentTypes = invocation.arguments.map { getCanonicalTypeFromObject(it) },
        arguments = listOf(*invocation.arguments),
        returnType = methodReturnType.toCanonical(),
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
        argumentTypes = invocation.arguments.map { getCanonicalTypeFromObject(it) },
        arguments = listOf(*invocation.arguments),
        returnType = methodReturnType.toCanonical(),
        exceptionType = exception.javaClass.name,
        exceptionMessage = exception.message,
        resultType = null,
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
        argumentTypes = invocation.arguments.map { getCanonicalTypeFromObject(it) },
        arguments = listOf(*invocation.arguments),
        returnType = methodReturnType.toCanonical(),
        exceptionType = exception.javaClass.name,
        exceptionMessage = exception.message,
    )
    storage.record(snap)
}

internal fun snapInvocation(
    storage: InvocationStorage,
    writer: SnapWriter,
    dependencies: Map<String, String>,
    joinPoint: ProceedingJoinPoint,
    result: Any?
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
        argumentTypes = args.map { getCanonicalTypeFromObject(it) },
        arguments = listOf(*args),
        returnType = returnType.toCanonical(),
        resultType = getCanonicalTypeFromObject(result),
        result = result,
        exceptionType = null,
        exceptionMessage = null,
    )
    val snap = SnapData(
        main = main,
        dependencies = dependencies,
        dependents = storage.getDependencyInvocations(),
        factories = storage.getFactoryInvocations()
    )
    writer.write(snap)
    storage.reset()
}

internal fun snapInvocationException(
    storage: InvocationStorage,
    writer: SnapWriter,
    dependencies: MutableMap<String, String>,
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
        argumentTypes = args.map { getCanonicalTypeFromObject(it) },
        arguments = listOf(*args),
        returnType = returnType.toCanonical(),
        exceptionType = throwable.javaClass.name,
        exceptionMessage = throwable.message,
        resultType = null,
        result = null,
    )
    val snap = SnapData(
        main = main,
        dependencies = dependencies,
        dependents = storage.getDependencyInvocations(),
        factories = storage.getFactoryInvocations()
    )
    writer.write(snap)
    storage.reset()
}

internal fun getCanonicalTypeFromObject(obj: Any?): String? {
    return when (obj) {
        null -> null
        is List<*> -> {
            val elementType = obj.firstOrNull()?.let { AopProxyUtils.ultimateTargetClass(it) }
            if (elementType != null) {
                typeFactory.constructCollectionType(List::class.java, elementType)
            } else {
                typeFactory.constructCollectionType(List::class.java, Any::class.java)
            }
        }
        is Set<*> -> {
            val elementType = obj.firstOrNull()?.let { AopProxyUtils.ultimateTargetClass(it) }
            if (elementType != null) {
                typeFactory.constructCollectionType(Set::class.java, elementType)
            } else {
                typeFactory.constructCollectionType(Set::class.java, Any::class.java)
            }
        }
        is Map<*, *> -> {
            val keyType = obj.keys.firstOrNull()?.let { AopProxyUtils.ultimateTargetClass(it) } ?: Any::class.java
            val valueType = obj.values.firstOrNull()?.let { AopProxyUtils.ultimateTargetClass(it) } ?: Any::class.java
            typeFactory.constructMapType(Map::class.java, keyType, valueType)
        }
        else -> typeFactory.constructType(AopProxyUtils.ultimateTargetClass(obj))
    }?.toCanonical()
}
