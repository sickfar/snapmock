package org.snapmock.snap.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.TypeFactory

object TestSupport {

    private val cache: MutableMap<Source, SnapFromSource> = mutableMapOf()
    private var objectMapperCustomizer: SnapMockObjectMapperCustomizer? = null
    private val objectMapper: ObjectMapper by lazy { objectMapper(objectMapperCustomizer) }
    private val typeFactory = TypeFactory.defaultInstance()
    private val reader: SnapReader by lazy { SnapReader(objectMapper) }

    @JvmStatic
    fun snap(source: Source) = cache.computeIfAbsent(source) { reader.read(it) }.snap

    @JvmStatic
    fun setObjectMapperCustomizer(customizer: SnapMockObjectMapperCustomizer) {
        objectMapperCustomizer = customizer
    }

    @JvmStatic
    fun <T> mainArg(source: Source, argIndex: Int): T? {
        val snap = snap(source).main
        val argumentType = snap.argumentTypes?.get(argIndex) ?: snap.parameterTypes[argIndex]
        val javaType = typeFactory.constructFromCanonical(argumentType)
        return objectMapper.convertValue(snap.arguments[argIndex], javaType)
    }

    @JvmStatic
    fun <T> mainResult(source: Source): T? {
        val snap = snap(source).main
        val returnType = snap.returnType
        val javaType = typeFactory.constructFromCanonical(returnType)
        return objectMapper.convertValue(snap.result, javaType)
    }

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun <T> mainThrClass(source: Source): Class<T> {
        val snap = snap(source).main
        return Class.forName(snap.exceptionType) as Class<T>
    }

    @JvmStatic
    fun mainThrMess(source: Source): String? {
        val snap = snap(source).main
        return snap.exceptionMessage
    }

    @JvmStatic
    fun <T> depArg(source: Source, depIndex: Int, argIndex: Int): T? {
        val snap = snap(source).dependencies[depIndex]
        val argumentType = snap.argumentTypes?.get(argIndex) ?: snap.parameterTypes[argIndex]
        val javaType = typeFactory.constructFromCanonical(argumentType)
        return objectMapper.convertValue(snap.arguments[argIndex], javaType)
    }

    @JvmStatic
    fun <T> factArg(source: Source, depIndex: Int, argIndex: Int): T? {
        val snap = snap(source).factories[depIndex]
        val argumentType = snap.argumentTypes?.get(argIndex) ?: snap.parameterTypes[argIndex]
        val javaType = typeFactory.constructFromCanonical(argumentType)
        return objectMapper.convertValue(snap.arguments[argIndex], javaType)
    }

    @JvmStatic
    fun <T> depResult(source: Source, depIndex: Int): T? {
        val snap = snap(source).dependencies[depIndex]
        val returnType = snap.returnType
        val javaType = typeFactory.constructFromCanonical(returnType)
        return objectMapper.convertValue(snap.result, javaType)
    }

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun <T> depThrClass(source: Source, depIndex: Int): Class<T> {
        val snap = snap(source).dependencies[depIndex]
        return Class.forName(snap.exceptionType) as Class<T>
    }

    @JvmStatic
    fun depThrMess(source: Source, depIndex: Int): String? {
        val snap = snap(source).dependencies[depIndex]
        return snap.exceptionMessage
    }

}
