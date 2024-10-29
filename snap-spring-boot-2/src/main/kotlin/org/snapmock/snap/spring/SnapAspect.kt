package org.snapmock.snap.spring

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.type.TypeFactory
import mu.KotlinLogging
import org.aopalliance.intercept.MethodInterceptor
import org.aopalliance.intercept.MethodInvocation
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.snapmock.snap.core.InvocationSnap
import org.snapmock.snap.core.InvocationStorage
import org.snapmock.snap.core.SnapData
import org.snapmock.snap.core.SnapWriter
import org.springframework.aop.framework.ProxyFactory
import org.springframework.stereotype.Component
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.optionals.getOrNull

private val log = KotlinLogging.logger {}

@Aspect
@Component
open class SnapAspect(
    private val writer: SnapWriter,
    private val storage: InvocationStorage
) {

    private val interceptedBeanCache: MutableMap<Class<*>, Any> = ConcurrentHashMap(50)
    private val typeFactory = TypeFactory.defaultInstance()

    @Pointcut("@annotation(org.snapmock.snap.core.Snap)")
    open fun onMethodAnnotatedSnap() {
    }

    @Pointcut("execution(public * *(..)) && within(@org.snapmock.snap.core.Snap *)")
    open fun onAnyMethodOfClassAnnotatedSnap() {
    }

    @Around("onAnyMethodOfClassAnnotatedSnap() || onMethodAnnotatedSnap()")
    open fun aroundMethod(joinPoint: ProceedingJoinPoint): Any {
        val signature = joinPoint.signature as MethodSignature
        val target = joinPoint.target
        val targetClass = target.javaClass
        val newTarget = interceptedBeanCache.computeIfAbsent(targetClass) { buildNewTarget(target, targetClass) }
        try {
            storage.start()
            val result = signature.method.invoke(newTarget, *joinPoint.args)
            snapInvocation(joinPoint, result)
            storage.stop()
            return result
        } catch (e: Throwable) {
            snapInvocationException(joinPoint, e)
            throw e
        }
    }

    private fun buildNewTarget(target: Any, targetClass: Class<*>): Any {
        val fields = targetClass.declaredFields
        // so far support only unique beans extraction
        val fieldValues: MutableMap<Class<*>, Any> = mutableMapOf()
        try {
            for (field in fields) {
                field.trySetAccessible()
                val type = field.type
                val value = field[target]
                fieldValues[type] = value
            }
        } catch (e: IllegalAccessException) {
            log.warn { "Cannot create intercepted object ${targetClass.name} because cannot access object fields" }
            return target
        }
        val maxCtor = Arrays.stream(targetClass.declaredConstructors)
            .filter { ctor: Constructor<*> -> Modifier.isPublic(ctor.modifiers) }
            .max(Comparator.comparingInt { obj: Constructor<*> -> obj.parameterCount })
            .getOrNull()
        if (maxCtor == null) {
            log.warn { "Cannot create intercepted object ${targetClass.name} because no available constructor found" }
            return target
        }
        val parameterTypes = maxCtor.parameterTypes
        val arguments = arrayOfNulls<Any>(parameterTypes.size)
        for (i in parameterTypes.indices) {
            val parameterType = parameterTypes[i]
            val unwrappedArgument = fieldValues[parameterType]
            val wrappedArgument = buildDependencySpy(parameterType, unwrappedArgument)
            check(parameterType.isInstance(wrappedArgument)) { "Argument $i is not instance of ${parameterType.name}" }
            arguments[i] = wrappedArgument
        }
        try {
            val result = maxCtor.newInstance(*arguments)
            log.debug { "New target built for bean ${targetClass.name}" }
            return result
        } catch (e: Exception) {
            log.warn {
                "Cannot create intercepted object ${targetClass.name} because cannot invoke constructor $maxCtor"
            }
            return target
        }
    }

    private fun buildDependencySpy(parameterType: Class<*>, dependency: Any?): Any? {
        if (dependency == null) {
            return null
        }
        val proxyFactory = ProxyFactory(dependency)
        proxyFactory.isProxyTargetClass = true
        proxyFactory.targetClass = parameterType
        proxyFactory.addAdvice(MethodInterceptor { invocation: MethodInvocation ->
            log.trace { "Dependency method invocation $invocation" }
            val method = invocation.method
            val args = invocation.arguments
            val isPublic = Modifier.isPublic(invocation.method.modifiers)
            log.trace { "Dependency method in public: $isPublic" }
            try {
                val result = method.invoke(dependency, *args)
                if (isPublic) {
                    snapDependencyInvocation(parameterType, invocation, result)
                }
                return@MethodInterceptor result
            } catch (e: InvocationTargetException) {
                if (isPublic) {
                    snapDependencyInvocationException(parameterType, invocation, e.targetException)
                }
                throw e
            }
        })
        val result = proxyFactory.proxy
        log.debug { "Dependency $parameterType has been successfully wrapped" }
        return result
    }

    private fun snapDependencyInvocation(dependencyType: Class<*>, invocation: MethodInvocation, result: Any) {
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

    private fun snapDependencyInvocationException(
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

    private fun snapInvocation(joinPoint: ProceedingJoinPoint, result: Any) {
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
            dependencies = storage.get()
        )
        writer.write(snap)
        storage.reset()
    }

    private fun snapInvocationException(joinPoint: ProceedingJoinPoint, throwable: Throwable) {
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
            dependencies = storage.get()
        )
        writer.write(snap)
        storage.reset()
    }
}
