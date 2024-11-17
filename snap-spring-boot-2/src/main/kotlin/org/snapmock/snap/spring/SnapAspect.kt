package org.snapmock.snap.spring

import io.github.oshai.kotlinlogging.KotlinLogging
import org.aopalliance.intercept.MethodInterceptor
import org.aopalliance.intercept.MethodInvocation
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.snapmock.core.*
import org.springframework.aop.framework.ProxyFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.lang.reflect.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors
import kotlin.jvm.optionals.getOrNull

private val log = KotlinLogging.logger {}

/**
 * This aspect processes beans and bean methods annotated [Snap]
 *
 * @since 1.0.0
 * @author Roman Aksenenko
 */
@Aspect
@Component
open class SnapAspect(
    private val writer: SnapWriter,
    private val storage: InvocationStorage,
    private val configuration: SnapConfigurationProperties,
    private val postprocessors: ObjectProvider<SnapResultPostprocessor<*>>
) {

    private val interceptedBeanCache: MutableMap<Class<*>, Any> = ConcurrentHashMap(50)

    @Pointcut("@annotation(org.snapmock.core.Snap)")
    open fun onMethodAnnotatedSnap() {
    }

    @Pointcut("execution(public * *(..)) && within(@org.snapmock.core.Snap *)")
    open fun onAnyMethodOfClassAnnotatedSnap() {
    }

    @Around("onAnyMethodOfClassAnnotatedSnap() || onMethodAnnotatedSnap()")
    open fun aroundMethod(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature as MethodSignature
        val target = joinPoint.target
        val targetClass = target.javaClass
        val dependencies = mutableMapOf<String, String>()
        val newTarget = interceptedBeanCache.computeIfAbsent(targetClass) { buildNewTarget(target, targetClass, dependencies) }
        try {
            storage.start()
            val result: Any? = signature.method.invoke(newTarget, *joinPoint.args).also { it: Any? ->
                postprocessResult(signature.returnType, it)
            }
            snapInvocation(storage, writer, dependencies, joinPoint, result)
            storage.stop()
            return result
        } catch (e: Throwable) {
            snapInvocationException(storage, writer, dependencies, joinPoint, e)
            throw e
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun postprocessResult(returnType: Class<*>, obj: Any?) {
        if (obj == null) {
            return
        }
        try {
            val postprocessor: SnapResultPostprocessor<in Any?>? = postprocessors.find { processor ->
                val superClass = processor.javaClass.genericInterfaces[0] as ParameterizedType
                superClass.actualTypeArguments[0] as Class<*> == returnType
            } as SnapResultPostprocessor<in Any?>?
            postprocessor?.accept(obj)
        } catch (e: Throwable) {
            log.error(e) { "Error calling postprocessor" }
        }
    }

    private fun buildNewTarget(oldTarget: Any, targetClass: Class<*>, dependencies: MutableMap<String, String>): Any {
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
            parameters.forEachIndexed { i, p ->
                if (p.isNamePresent) {
                    dependencies[p.name] = p.type.name
                } else {
                    dependencies[i.toString()] = p.type.name
                }
            }
            log.debug { "New target built for bean ${targetClass.name}" }
            setNewTargetAutowiredSetters(targetClass, oldTarget, newTarget, dependencies)
            log.debug { "Autowired setters have been set for bean ${targetClass.name}" }
            setNewTargetAutowiredFields(targetClass, oldTarget, newTarget, dependencies)
            log.debug { "Autowired fields have been set for bean ${targetClass.name}" }
            return newTarget
        } catch (e: InvocationTargetException) {
            log.warn(e) {
                "Cannot create intercepted object ${targetClass.name} because cannot invoke method"
            }
            return oldTarget
        } catch (_: IllegalAccessException) {
            log.warn { "Cannot create intercepted object ${targetClass.name} because cannot access object members" }
            return oldTarget
        } catch (e: IllegalStateException) {
            log.warn(e) {
                "Cannot create intercepted object ${targetClass.name} because of failed preconditions"
            }
            return oldTarget
        }
    }

    private fun buildDependencySpy(parameterType: Class<*>, dependency: Any?, isFactoryByFieldOrCtorArg: Boolean): Any? {
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
                val result: Any? = method.invoke(dependency, *args)
                if (method.isAnnotationPresent(SnapDepFactory::class.java) ||
                    ((parameterType.isAnnotationPresent(SnapDepFactory::class.java) || isFactoryByFieldOrCtorArg) && isPublic)) {
                    snapFactoryInvocation(storage, parameterType, invocation)
                    return@MethodInterceptor buildDependencySpy(method.returnType, result, false)
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

    private fun getOldTargetFieldValues(target: Any, targetClass: Class<*>): Map<Class<*>, Pair<Field, Any?>> {
        return Arrays.stream(targetClass.declaredFields)
            .peek { it.trySetAccessible() }
            .collect(Collectors.toMap({ it.type }, { it to it[target] }))
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
                    val isFactoryAnnotationPresent = correspondingField.isAnnotationPresent(SnapDepFactory::class.java) || parameters[i].isAnnotationPresent(SnapDepFactory::class.java)
                    val wrappedArgument = buildDependencySpy(parameterType, unwrappedArgument, isFactoryAnnotationPresent)
                    check(parameterType.isInstance(wrappedArgument)) { "For class ${targetClass.name} constructor argument $i is not instance of ${parameterType.name}" }
                    arguments[i] = wrappedArgument
                } catch (_: NoSuchFieldException) {
                    throw IllegalStateException("For class ${targetClass.name} constructor argument $parameterName type ${parameterType.name} is not found in fields. Such bean cannot be intercepted to snap")
                }
            }
        } else {
            // if no param names then we can only operate types, which does not allow duplicates
            val fieldValues = getOldTargetFieldValues(oldTarget, targetClass)
            for (i in parameters.indices) {
                val parameterType = parameters[i].type
                check(fieldValues.containsKey(parameterType)) { "For class ${targetClass.name} constructor argument $i of type ${parameterType.name} is not found in fields. Such bean cannot be intercepted to snap" }
                val fieldAndArgument = fieldValues[parameterType]!!
                val unwrappedArgument = fieldAndArgument.second
                val isFactoryAnnotationPresent = fieldAndArgument.first.isAnnotationPresent(SnapDepFactory::class.java) || parameters[i].isAnnotationPresent(SnapDepFactory::class.java)
                val wrappedArgument = buildDependencySpy(parameterType, unwrappedArgument, isFactoryAnnotationPresent)
                check(parameterType.isInstance(wrappedArgument)) { "For class ${targetClass.name} constructor argument $i is not instance of ${parameterType.name}" }
                arguments[i] = wrappedArgument
            }
        }
        return arguments
    }

    private fun setNewTargetAutowiredSetters(
        targetClass: Class<*>,
        oldTarget: Any,
        newTarget: Any,
        dependencies: MutableMap<String, String>
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
                    val isFactoryAnnotationPresent = correspondingField.isAnnotationPresent(SnapDepFactory::class.java) || method.isAnnotationPresent(SnapDepFactory::class.java)
                    val wrappedArgument = buildDependencySpy(propertyType, arg, isFactoryAnnotationPresent)
                    method.invoke(newTarget, wrappedArgument)
                    dependencies[propertyName] = propertyType.name
                } catch (_: NoSuchFieldException) {
                    throw IllegalStateException("For class ${targetClass.name} property $propertyName of type ${propertyType.name} is not found in fields. Such bean cannot be intercepted to snap")
                }
            }
    }

    private fun setNewTargetAutowiredFields(
        targetClass: Class<*>,
        oldTarget: Any,
        newTarget: Any,
        dependencies: MutableMap<String, String>
    ) {
        Arrays.stream(targetClass.declaredFields)
            .filter { it.isAnnotationPresent(Autowired::class.java) }
            .peek { it.trySetAccessible() }
            .forEach { field ->
                val fieldValue = field.get(oldTarget)
                val isFactoryAnnotationPresent = field.isAnnotationPresent(SnapDepFactory::class.java)
                val wrappedFieldValue = buildDependencySpy(field.type, fieldValue, isFactoryAnnotationPresent)
                field.set(newTarget, wrappedFieldValue)
                dependencies[field.name] = field.type.name
            }
    }
}
