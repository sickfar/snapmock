package org.snapmock.mock.mockito

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.kotlin.KStubbing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.snapmock.core.SnapData
import org.snapmock.core.Source
import org.snapmock.core.TestSupport
import java.lang.reflect.Constructor
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Function
import java.util.logging.Logger
import java.util.stream.Collectors

fun interface AssertionBiConsumer<E, A> : BiConsumer<E, A> {
    override fun accept(expected: E, actual: A)
}

object MockitoMockSupport {

    private val log = Logger.getLogger("MockitoMockSupport")

    @JvmStatic
    fun configureMocks(test: Any, source: Source) {
        val snap = TestSupport.snap(source)
        val mockObjects: MutableMap<Class<*>, Any> = Arrays.stream(test.javaClass.declaredFields)
            .filter { it.isAnnotationPresent(Mock::class.java) }
            .peek { it.trySetAccessible() }
            .collect(
                Collectors.toMap(
                    { it.type },
                    { it.get(test) },
                    { k, _ -> throw IllegalStateException("Multiple fields of same type found: ${k.javaClass.name}") },
                    { mutableMapOf() })
            )
        log.fine { "Collected ${mockObjects.size} mocks from test class" }
        configureMocks0(mockObjects, snap, source)
    }

    @JvmStatic
    fun configureMocks(source: Source): Map<Class<*>, Any> {
        val snap = TestSupport.snap(source)
        val mockObjects: MutableMap<Class<*>, Any> = snap.dependencies.map {
            val aClass = Class.forName(it.value)
            aClass to Mockito.mock(aClass)
        }.toMap().toMutableMap()
        log.fine { "No test class passed. Mocks will be created dynamically" }
        configureMocks0(mockObjects, snap, source)
        return mockObjects
    }

    // TODO consider changing computeIfAbsent to avoid side effects
    @Suppress("UNCHECKED_CAST")
    private fun configureMocks0(
        mockObjects: MutableMap<Class<*>, Any>,
        snap: SnapData,
        source: Source
    ) {
        snap.factories.forEachIndexed { depIndex, factory ->
            val dependencyClass = Class.forName(factory.className)
            // we add factory produced dependency
            val mockObject = mockObjects[dependencyClass]
            checkNotNull(mockObject) { "Mock object for class ${dependencyClass.name} is not found" }
            val parameterTypes = factory.parameterTypes.map { Class.forName(it) }.toTypedArray()
            val mockMethod = dependencyClass.getMethod(factory.methodName, *parameterTypes)
            val resultClass = Class.forName(factory.returnType)
            KStubbing(mockObject).apply {
                on {
                    mockMethod.invoke(this, *mapFactoryArgs(factory.arguments.size, source, depIndex))
                } doReturn mockObjects.computeIfAbsent(resultClass) { Mockito.mock(resultClass) }
            }
        }
        snap.dependents.forEachIndexed { depIndex, dependency ->
            val dependencyClass = Class.forName(dependency.className)
            val mockObject = mockObjects[dependencyClass]
            checkNotNull(mockObject) { "Mock object for class ${dependencyClass.name} is not found" }
            val parameterTypes = dependency.parameterTypes.map { Class.forName(it) }.toTypedArray()
            val mockMethod = dependencyClass.getMethod(dependency.methodName, *parameterTypes)
            if (dependency.exceptionType != null) {
                val exceptionType: Class<Throwable> = Class.forName(dependency.exceptionType) as Class<Throwable>
                val exceptionMessage = dependency.exceptionMessage
                if (exceptionMessage != null) {
                    val exception = MockitoTestSupport.depThr(exceptionType, exceptionMessage)
                    KStubbing(mockObject).apply {
                        on {
                            mockMethod.invoke(this, *mapDepArgs(dependency.arguments.size, source, depIndex))
                        } doThrow exception
                    }
                } else {
                    KStubbing(mockObject).apply {
                        on {
                            mockMethod.invoke(this, *mapDepArgs(dependency.arguments.size, source, depIndex))
                        }.thenThrow(exceptionType)
                    }
                }
            } else {
                KStubbing(mockObject).apply {
                    on {
                        mockMethod.invoke(this, *mapDepArgs(dependency.arguments.size, source, depIndex))
                    } doReturn TestSupport.depResult(source, depIndex)
                }
            }
        }
    }

    @JvmStatic
    fun <E, A> doSnapshotTest(
        test: Any,
        source: Source,
        expectedConverter: Function<Any?, E>?,
        asserts: AssertionBiConsumer<E, A>
    ) {
        val snap = TestSupport.snap(source)
        configureMocks(test, source)
        val subjectClass = Class.forName(snap.main.className)
        val subject = Arrays.stream(test.javaClass.declaredFields)
            .filter { it.isAnnotationPresent(InjectMocks::class.java) }
            .filter { it.type == subjectClass }
            .peek { it.trySetAccessible() }
            .map { it.get(test) }
            .findFirst()
            .orElseThrow { IllegalStateException("No field of type ${snap.main.className} annotated @InjectMocks found") }
        doSnapshotTest0(snap, subjectClass, source, subject, expectedConverter, asserts)
    }

    @JvmStatic
    fun <E, A> doSnapshotTest(
        source: Source,
        expectedConverter: Function<Any?, E>?,
        asserts: AssertionBiConsumer<E, A>
    ) {
        val snap = TestSupport.snap(source)
        val mocks = configureMocks(source)
        val subjectClass = Class.forName(snap.main.className)
        val subject = buildSubject(subjectClass, mocks)
        doSnapshotTest0(snap, subjectClass, source, subject, expectedConverter, asserts)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <A, E> doSnapshotTest0(
        snap: SnapData,
        subjectClass: Class<*>,
        source: Source,
        subject: Any?,
        expectedConverter: Function<Any?, E>?,
        asserts: AssertionBiConsumer<E, A>
    ) {
        val parameterTypes = snap.main.parameterTypes.map { Class.forName(it) }.toTypedArray()
        val subjectMethod = subjectClass.getMethod(snap.main.methodName, *parameterTypes)
        val args = List(snap.main.arguments.size) { index ->
            TestSupport.subjArg<Any>(source, index)
        }.toTypedArray()
        if (snap.main.exceptionType != null) {
            val exception = assertThrows<Throwable> {
                subjectMethod.invoke(subject, *args)
            }
            if (snap.main.exceptionMessage != null) {
                assertEquals(snap.main.exceptionMessage, exception.message)
            }
        } else {
            val expected = if (expectedConverter == null) {
                TestSupport.subjResult<Any>(source)
            } else {
                expectedConverter.apply(snap.main.result)
            }
            val actual = subjectMethod.invoke(subject, *args)
            asserts.accept(expected as E, actual as A)
        }
    }

    @JvmStatic
    fun <E, A> doSnapshotTest(test: Any, source: Source, asserts: AssertionBiConsumer<E, A>) {
        doSnapshotTest(test, source, null, asserts)
    }

    @JvmStatic
    fun doSnapshotTest(test: Any, source: Source) {
        doSnapshotTest(test, source, null) { expected: Any, actual: Any -> assertEquals(expected, actual) }
    }

    @JvmStatic
    fun <E, A> doSnapshotTest(
        source: Source,
        asserts: AssertionBiConsumer<E, A>
    ) {
        doSnapshotTest(source, null, asserts)
    }

    @JvmStatic
    fun doSnapshotTest(
        source: Source
    ) {
        doSnapshotTest(source, null) { expected: Any, actual: Any ->
            assertEquals(
                expected,
                actual
            )
        }
    }

    private fun mapDepArgs(size: Int, source: Source, depIndex: Int): Array<Any?> {
        val args = arrayOfNulls<Any>(size)
        for (argIndex in args.indices) {
            val arg = TestSupport.depArg<Any>(source, depIndex, argIndex)
            args[argIndex] = ArgumentMatchers.eq(arg)
        }
        return args
    }

    private fun mapFactoryArgs(size: Int, source: Source, depIndex: Int): Array<Any?> {
        val args = arrayOfNulls<Any>(size)
        for (argIndex in args.indices) {
            val arg = TestSupport.factArg<Any>(source, depIndex, argIndex)
            args[argIndex] = ArgumentMatchers.eq(arg)
        }
        return args
    }

    private fun buildSubject(
        subjectClass: Class<*>,
        dependencies: Map<Class<*>, Any>
    ): Any {
        val classes = dependencies.keys
        val ctor = Arrays.stream(subjectClass.declaredConstructors)
            // longest constructor first
            .sorted(Comparator.comparingInt(Constructor<*>::getParameterCount).reversed())
            // filter constructors with unique arguments
            .filter { it.parameterCount == it.parameterTypes.toSet().size }
            .filter { classes.containsAll(it.parameterTypes.toSet()) }
            .findFirst()
            .or {
                log.fine { "No fully matching constructor found. Will try to find constructor which contains as much dependencies as possible and mock extra arguments" }
                Arrays.stream(subjectClass.declaredConstructors)
                    .sorted(Comparator.comparingInt { ctor -> classes.count { it in ctor.parameterTypes } })
                    .findFirst()
            }
            .orElseThrow {
                IllegalStateException(
                    """No suitable constructor found for ${subjectClass.name} and dependency types ${
                        classes.joinToString(
                            ","
                        ) { it.name }
                    }"""
                )
            }
        val arguments = arrayOfNulls<Any>(ctor.parameterCount)
        ctor.parameterTypes.forEachIndexed { i, parameterType ->
            arguments[i] = dependencies[parameterType]
        }
        ctor.trySetAccessible()
        val subject = ctor.newInstance(*arguments)
        val notUsedDependencies = dependencies.filter { !ctor.parameterTypes.contains(it.key) }
        notUsedDependencies.forEach { type, dep ->
            Arrays.stream(subjectClass.declaredFields)
                .filter { it.type == type }
                .findFirst()
                .ifPresentOrElse({
                    // found
                    it.trySetAccessible()
                    it[subject] = dep
                }, {
                    Arrays.stream(subjectClass.declaredMethods)
                        .filter { it.name.startsWith("set") }
                        .filter { it.parameterCount == 1 }
                        .filter { it.parameterTypes[0] == type }
                        .findFirst()
                        .ifPresent {
                            it.trySetAccessible()
                            it.invoke(subject, dep)
                        }
                })
        }
        return subject
    }
}
