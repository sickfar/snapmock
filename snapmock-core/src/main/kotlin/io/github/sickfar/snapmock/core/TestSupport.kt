package io.github.sickfar.snapmock.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.TypeFactory
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

/**
 * Method to support using created snapshots in unit tests
 *
 * Snapshots are being cached after reading for better performance
 *
 * @since 1.0.0
 * @author Roman Aksenenko
 */
object TestSupport {

    private val cache: MutableMap<Source, SnapFromSource> = mutableMapOf()
    private val typeFactory = TypeFactory.defaultInstance()
    private var objectMapperCustomizer: SnapMockObjectMapperCustomizer? = null
    private val objectMapper: ObjectMapper by lazy { objectMapper(objectMapperCustomizer) }
    private val reader: SnapReader by lazy { SnapReader(objectMapper) }

    /**
     * Set static ObjectMapper customizer (for all tests in JVM instance)
     * @param customizer ObjectMapper customizer
     *
     * @see SnapMockObjectMapperCustomizer
     */
    @JvmStatic
    fun setObjectMapperCustomizer(customizer: SnapMockObjectMapperCustomizer) {
        objectMapperCustomizer = customizer
    }

    /**
     * Read a snapshot from a source
     * @param source Source to read a snapshot from
     * @return Snapshot
     */
    @JvmStatic
    fun snap(source: Source) =
        cache.computeIfAbsent(source) { s -> reader.read(s).also { log.debug { "Snap read from ${s.name}" } } }.snap

    /**
     * Read a test subject invocation argument of given index
     * @param source Source to read a snapshot and subject invocation argument
     * @param argIndex Index of argument to read
     * @return Read argument. The type of argument is determined by [InvocationSnap.argumentTypes] or [InvocationSnap.parameterTypes]
     * (in priority of order)
     */
    @JvmStatic
    fun <T> subjArg(source: Source, argIndex: Int): T? {
        val snap = snap(source).main
        if (snap.arguments[argIndex] == null) {
            return null
        }
        val argumentType = snap.argumentTypes?.get(argIndex) ?: snap.parameterTypes[argIndex]
        val javaType = typeFactory.constructFromCanonical(argumentType)
        log.debug { "Reading subject invocation argument $argIndex of type $argumentType from source ${source.name}" }
        return objectMapper.convertValue(snap.arguments[argIndex], javaType)
    }

    /**
     * Read a test subject invocation result
     * @param source Source to read a snapshot and subject invocation result
     * @return Read result. The type of result is determined by [InvocationSnap.resultType]
     */
    @JvmStatic
    fun <T> subjResult(source: Source): T? {
        val snap = snap(source).main
        if (snap.result == null) {
            return null
        }
        val resultType = snap.resultType
        val javaType = typeFactory.constructFromCanonical(resultType)
        log.debug { "Reading subject invocation result of type $resultType from source ${source.name}" }
        return objectMapper.convertValue(snap.result, javaType)
    }

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun <T> subjThrClass(source: Source): Class<T> {
        log.debug { "Reading subject invocation thrown class from source ${source.name}" }
        val snap = snap(source).main
        return Class.forName(snap.exceptionType) as Class<T>
    }

    @JvmStatic
    fun subjThrMess(source: Source): String? {
        log.debug { "Reading subject invocation thrown message from source ${source.name}" }
        val snap = snap(source).main
        return snap.exceptionMessage
    }

    @JvmStatic
    fun <T> depArg(source: Source, depIndex: Int, argIndex: Int): T? {
        val snap = snap(source).dependents[depIndex]
        if (snap.arguments[argIndex] == null) {
            return null
        }
        val argumentType = snap.argumentTypes?.get(argIndex) ?: snap.parameterTypes[argIndex]
        val javaType = typeFactory.constructFromCanonical(argumentType)
        log.debug { "Reading dependency $depIndex invocation argument $argIndex of type $javaType from source ${source.name}" }
        return objectMapper.convertValue(snap.arguments[argIndex], javaType)
    }

    @JvmStatic
    fun <T> factArg(source: Source, depIndex: Int, argIndex: Int): T? {
        val snap = snap(source).factories[depIndex]
        if (snap.arguments[argIndex] == null) {
            return null
        }
        val argumentType = snap.argumentTypes?.get(argIndex) ?: snap.parameterTypes[argIndex]
        val javaType = typeFactory.constructFromCanonical(argumentType)
        log.debug { "Reading dependency $depIndex factory invocation argument $argIndex of type $javaType from source ${source.name}" }
        return objectMapper.convertValue(snap.arguments[argIndex], javaType)
    }

    @JvmStatic
    fun <T> depResult(source: Source, depIndex: Int): T? {
        val snap = snap(source).dependents[depIndex]
        if (snap.result == null) {
            return null
        }
        val resultType = snap.resultType
        val javaType = typeFactory.constructFromCanonical(resultType)
        log.debug { "Reading dependency $depIndex invocation result of type $resultType from source ${source.name}" }
        return objectMapper.convertValue(snap.result, javaType)
    }

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun <T> depThrClass(source: Source, depIndex: Int): Class<T> {
        val snap = snap(source).dependents[depIndex]
        log.debug { "Reading dependency $depIndex invocation thrown exception class from source ${source.name}" }
        return Class.forName(snap.exceptionType) as Class<T>
    }

    @JvmStatic
    fun depThrMess(source: Source, depIndex: Int): String? {
        val snap = snap(source).dependents[depIndex]
        log.debug { "Reading dependency $depIndex invocation thrown exception message from source ${source.name}" }
        return snap.exceptionMessage
    }

}
