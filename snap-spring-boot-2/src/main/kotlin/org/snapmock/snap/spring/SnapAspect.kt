package org.snapmock.snap.spring

import mu.KotlinLogging
import org.aopalliance.intercept.MethodInterceptor
import org.aopalliance.intercept.MethodInvocation
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.snapmock.core.InvocationStorage
import org.snapmock.core.Snap
import org.snapmock.core.SnapDepFactory
import org.snapmock.core.SnapWriter
import org.springframework.aop.framework.ProxyFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier
import java.lang.reflect.Parameter
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors
import kotlin.jvm.optionals.getOrNull

private val log = KotlinLogging.logger {}

/**
 * This aspect processes beans and bean methods annotated [Snap]
 */
@Aspect
@Component
open class SnapAspect(
    private val writer: SnapWriter,
    private val storage: InvocationStorage,
    private val configuration: SnapConfigurationProperties,
) {

    private val interceptedBeanCache: MutableMap<Class<*>, Any> = ConcurrentHashMap(50)

    @Pointcut("@annotation(org.snapmock.core.Snap)")
    open fun onMethodAnnotatedSnap() {
    }

    @Pointcut("execution(public * *(..)) && within(@org.snapmock.core.Snap *)")
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
            snapInvocation(storage, writer, joinPoint, result)
            storage.stop()
            return result
        } catch (e: Throwable) {
            snapInvocationException(storage, writer, joinPoint, e)
            throw e
        }
    }

    private fun buildNewTarget(oldTarget: Any, targetClass: Class<*>): Any {
        try {
            val maxCtor = Arrays.stream(targetClass.declaredConstructors)
                .filter { ctor: Constructor<*> -> Modifier.isPublic(ctor.modifiers) }
                .max(Comparator.comparingInt { obj: Constructor<*> -> obj.parameterCount })
                .getOrNull()
            if (maxCtor == null) {
                log.warn { "Cannot create intercepted object ${targetClass.name} because no available constructor found" }
                return oldTarget
            }
            val parameters = maxCtor.parameters
            val arguments = buildNewTargetCtorArguments(targetClass, oldTarget, parameters)
            val newTarget: Any = maxCtor.newInstance(*arguments)
            log.debug { "New target built for bean ${targetClass.name}" }
            setNewTargetAutowiredSetters(targetClass, oldTarget, newTarget)
            log.debug { "Autowired setters have been set for bean ${targetClass.name}" }
            setNewTargetAutowiredFields(targetClass, oldTarget, newTarget)
            log.debug { "Autowired fields have been set for bean ${targetClass.name}" }
            return newTarget
        } catch (e: InvocationTargetException) {
            log.warn(e) {
                "Cannot create intercepted object ${targetClass.name} because cannot invoke method"
            }
            return oldTarget
        } catch (e: IllegalAccessException) {
            log.warn { "Cannot create intercepted object ${targetClass.name} because cannot access object members" }
            return oldTarget
        } catch (e: IllegalStateException) {
            log.warn(e) {
                "Cannot create intercepted object ${targetClass.name} because of failed preconditions"
            }
            return oldTarget
        }
    }

    private fun buildDependencySpy(parameterType: Class<*>, dependency: Any?): Any? {
        if (dependency == null) {
            return null
        }
        if (configuration.ignore.mappedClasses.contains(parameterType)) {
            log.trace { "Ignored class ${parameterType.name} found, will not proxy" }
            return dependency
        }
        val proxyFactory = ProxyFactory(dependency)
        proxyFactory.isProxyTargetClass = true
        proxyFactory.targetClass = parameterType
        proxyFactory.addAdvice(MethodInterceptor { invocation: MethodInvocation ->
            log.trace { "Dependency method invocation $invocation" }
            val method = invocation.method
            val args = invocation.arguments
            val isPublic = Modifier.isPublic(invocation.method.modifiers)
            log.trace { "Dependency method is public: $isPublic" }
            try {
                val result = method.invoke(dependency, *args)
                if (method.isAnnotationPresent(SnapDepFactory::class.java) ||
                    (parameterType.isAnnotationPresent(SnapDepFactory::class.java) && isPublic)) {
                    snapFactoryInvocation(storage, parameterType, invocation)
                    return@MethodInterceptor buildDependencySpy(parameterType, result)
                } else if (isPublic) {
                    snapDependencyInvocation(storage, parameterType, invocation, result)
                }
                return@MethodInterceptor result
            } catch (e: InvocationTargetException) {
                if (method.isAnnotationPresent(SnapDepFactory::class.java)) {
                    snapFactoryInvocationException(storage, parameterType, invocation, e)
                } else if (isPublic) {
                    snapDependencyInvocationException(storage, parameterType, invocation, e.targetException)
                }
                throw e
            }
        })
        val result = proxyFactory.proxy
        log.debug { "Dependency $parameterType has been successfully wrapped" }
        return result
    }

    private fun getOldTargetFieldValues(target: Any, targetClass: Class<*>): Map<Class<*>, Any?> {
        return Arrays.stream(targetClass.declaredFields)
            .peek { it.trySetAccessible() }
            .collect(Collectors.toMap({ it.type }, { it[target] }))
    }

    private fun buildNewTargetCtorArguments(
        targetClass: Class<*>,
        oldTarget: Any,
        parameters: Array<Parameter>,
    ): Array<Any?> {
        val arguments = arrayOfNulls<Any>(parameters.size)
        if (parameters.all { it.isNamePresent }) {
            // if we have parameters names then set them from field names (allows type duplicates)
            for (i in parameters.indices) {
                val parameterName = parameters[i].name
                val parameterType = parameters[i].type
                try {
                    val correspondingField = targetClass.getDeclaredField(parameterName)
                    check(parameterType == correspondingField.type) { "For class ${targetClass.name} corresponding field $parameterName is not instance of ${parameterType.name}. Such bean cannot be intercepted to snap" }
                    correspondingField.trySetAccessible()
                    val unwrappedArgument = correspondingField.get(oldTarget)
                    val wrappedArgument = buildDependencySpy(parameterType, unwrappedArgument)
                    check(parameterType.isInstance(wrappedArgument)) { "For class ${targetClass.name} constructor argument $i is not instance of ${parameterType.name}" }
                    arguments[i] = wrappedArgument
                } catch (e: NoSuchFieldException) {
                    throw IllegalStateException("For class ${targetClass.name} constructor argument $parameterName type ${parameterType.name} is not found in fields. Such bean cannot be intercepted to snap")
                }
            }
        } else {
            // if no param names then we can only operate types, which does not allow duplicates
            val fieldValues = getOldTargetFieldValues(oldTarget, targetClass)
            for (i in parameters.indices) {
                val parameterType = parameters[i].type
                check(fieldValues.containsKey(parameterType)) { "For class ${targetClass.name} constructor argument $i of type ${parameterType.name} is not found in fields. Such bean cannot be intercepted to snap" }
                val unwrappedArgument = fieldValues[parameterType]
                val wrappedArgument = buildDependencySpy(parameterType, unwrappedArgument)
                check(parameterType.isInstance(wrappedArgument)) { "For class ${targetClass.name} constructor argument $i is not instance of ${parameterType.name}" }
                arguments[i] = wrappedArgument
            }
        }
        return arguments
    }

    private fun setNewTargetAutowiredSetters(
        targetClass: Class<*>,
        oldTarget: Any,
        newTarget: Any
    ) {
        Arrays.stream(targetClass.declaredMethods)
            .filter { it.isAnnotationPresent(Autowired::class.java) }
            .filter { it.name.startsWith("set") }
            .filter { it.parameterCount == 1 }
            .peek { it.trySetAccessible() }
            .forEach { method ->
                val propertyName = method.name.replace("set", "").replaceFirstChar { it.lowercaseChar() }
                val propertyType = method.parameterTypes[0]
                try {
                    val correspondingField = targetClass.getDeclaredField(propertyName)
                    check(propertyType == correspondingField.type) { "For class ${targetClass.name} corresponding field for property $propertyName is not instance of ${propertyType.name}. Such bean cannot be intercepted to snap" }
                    correspondingField.trySetAccessible()
                    val arg = correspondingField[oldTarget]
                    val wrappedArgument = buildDependencySpy(propertyType, arg)
                    method.invoke(newTarget, wrappedArgument)
                } catch (e: NoSuchFieldException) {
                    throw IllegalStateException("For class ${targetClass.name} property $propertyName of type ${propertyType.name} is not found in fields. Such bean cannot be intercepted to snap")
                }
            }
    }

    private fun setNewTargetAutowiredFields(targetClass: Class<*>, oldTarget: Any, newTarget: Any) {
        Arrays.stream(targetClass.declaredFields)
            .filter { it.isAnnotationPresent(Autowired::class.java) }
            .peek { it.trySetAccessible() }
            .forEach { field ->
                val fieldValue = field.get(oldTarget)
                val wrappedFieldValue = buildDependencySpy(field.type, fieldValue)
                field.set(newTarget, wrappedFieldValue)
            }
    }
}
