package org.snapmock.mock.mockito

import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.Assertions.*
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
import java.util.stream.Collectors

private val log = KotlinLogging.logger {}

fun interface AssertionBiConsumer<E, A> : BiConsumer<E?, A?> {
    override fun accept(expected: E?, actual: A?)
}

object MockitoMockSupport {

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
        log.debug { "Collected ${mockObjects.size} mocks from test class" }
        configureMocks0(mockObjects, snap, source)
    }

    @JvmStatic
    fun configureMocks(source: Source): Map<Class<*>, Any> {
        val snap = TestSupport.snap(source)
        val mockObjects: MutableMap<Class<*>, Any> = snap.dependencies.map {
            val aClass = Class.forName(it.value)
            aClass to Mockito.mock(aClass)
        }.toMap().toMutableMap()
        log.debug { "No test class passed. Mocks will be created dynamically" }
        configureMocks0(mockObjects, snap, source)
        return mockObjects
    }

    @Suppress("UNCHECKED_CAST")
    private fun configureMocks0(
        mockObjects: MutableMap<Class<*>, Any>,
        snap: SnapData,
        source: Source
    ) {
        log.info { "Configuring mocks for source ${source.name}" }
        snap.factories.forEachIndexed { depIndex, factory ->
            log.trace { "Mocking factory invocation #$depIndex for factory $factory" }
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
            log.info { "Stubbed dependency factory ${factory.className}" }
        }
        snap.dependents.forEachIndexed { depIndex, dependency ->
            log.trace { "Mocking dependency invocation #$depIndex for dependency $dependency" }
            val dependencyClass = Class.forName(dependency.className)
            val mockObject = mockObjects[dependencyClass]
            checkNotNull(mockObject) { "Mock object for class ${dependencyClass.name} is not found" }
            val parameterTypes = dependency.parameterTypes.map { Class.forName(it) }.toTypedArray()
            val mockMethod = dependencyClass.getMethod(dependency.methodName, *parameterTypes)
            if (dependency.exceptionType != null) {
                log.trace { "Mocking exception flow" }
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
                log.info { "Stubbed dependency invocation ${dependency.className} to throw ${dependency.exceptionType}" }
            } else {
                log.trace { "Mocking return flow" }
                KStubbing(mockObject).apply {
                    on {
                        mockMethod.invoke(this, *mapDepArgs(dependency.arguments.size, source, depIndex))
                    } doReturn TestSupport.depResult(source, depIndex)
                }
                log.info { "Stubbed dependency invocation ${dependency.className} #$depIndex to return result from ${source.name}" }
            }
        }
    }

    @JvmStatic
    fun <E, A> doSnapshotTestFromFields(
        test: Any,
        source: Source,
        expectedConverter: Function<Any?, E>?,
        asserts: AssertionBiConsumer<E, A>
    ) {
        log.info { "Performing snapshot test for class ${test.javaClass.name} from source ${source.name}" }
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
        log.debug { "Test subject found in class fields: $subject" }
        doSnapshotTest0(snap, subjectClass, source, subject, expectedConverter, asserts)
    }

    @JvmStatic
    fun <E, A> doSnapshotTest(
        source: Source,
        expectedConverter: Function<Any?, E>?,
        asserts: AssertionBiConsumer<E, A>
    ) {
        log.info { "Performing snapshot test from source ${source.name}" }
        val snap = TestSupport.snap(source)
        val mocks = configureMocks(source)
        val subjectClass = Class.forName(snap.main.className)
        val subject = buildSubject(subjectClass, mocks)
        log.debug { "Test subject built from mocks: $subject" }
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
        log.trace { "Subject invocation arguments built: " + args.contentDeepToString() }
        if (snap.main.exceptionType != null) {
            log.trace { "Expecting exception flow" }
            val exception = assertThrows<Throwable> {
                subjectMethod.invoke(subject, *args)
            }
            if (snap.main.exceptionMessage != null) {
                assertEquals(snap.main.exceptionMessage, exception.message)
            }
        } else {
            log.trace { "Expecting value flow" }
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
    fun <E, A> doSnapshotTestFromFields(test: Any, source: Source, asserts: AssertionBiConsumer<E, A>) {
        doSnapshotTestFromFields(test, source, null, asserts)
    }

    @JvmStatic
    fun doSnapshotTestFromFields(test: Any, source: Source) {
        doSnapshotTestFromFields(test, source, null) { expected: Any?, actual: Any? ->
            if (expected == null) {
                assertNull(actual)
            }
            assertEquals(expected, actual)
        }
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
        doSnapshotTest(source, null) { expected: Any?, actual: Any? ->
            if (expected == null) {
                assertNull(actual)
            }
            if (actual is Array<*>) {
                assertArrayEquals(expected as Array<*>, actual)
            } else {
                assertEquals(expected, actual)
            }
        }
    }

    private fun mapDepArgs(size: Int, source: Source, depIndex: Int): Array<Any?> {
        log.trace { "Mapping dependency arguments for dependency #$depIndex from source ${source.name}" }
        val args = arrayOfNulls<Any>(size)
        for (argIndex in args.indices) {
            val arg = TestSupport.depArg<Any>(source, depIndex, argIndex)
            log.trace { "Mapped dependency argument #$argIndex for dependency #$depIndex from source ${source.name}: $arg" }
            args[argIndex] = ArgumentMatchers.eq(arg)
        }
        return args
    }

    private fun mapFactoryArgs(size: Int, source: Source, depIndex: Int): Array<Any?> {
        log.trace { "Mapping dependency arguments for factory #$depIndex from source ${source.name}" }
        val args = arrayOfNulls<Any>(size)
        for (argIndex in args.indices) {
            val arg = TestSupport.factArg<Any>(source, depIndex, argIndex)
            log.trace { "Mapped factory argument #$argIndex for dependency #$depIndex from source ${source.name}: $arg" }
            args[argIndex] = ArgumentMatchers.eq(arg)
        }
        return args
    }

    private fun buildSubject(
        subjectClass: Class<*>,
        dependencies: Map<Class<*>, Any>
    ): Any {
        log.debug { "Building test subject from class ${subjectClass.name}" }
        val classes = dependencies.keys
        val ctor = Arrays.stream(subjectClass.declaredConstructors)
            // longest constructor first
            .sorted(Comparator.comparingInt(Constructor<*>::getParameterCount).reversed())
            // filter constructors with unique arguments
            .filter { it.parameterCount == it.parameterTypes.toSet().size }
            .filter { classes.containsAll(it.parameterTypes.toSet()) }
            .findFirst()
            .or {
                log.debug { "No fully matching constructor found. Will try to find constructor which contains as much dependencies as possible and mock extra arguments" }
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
        log.trace { "Found applicable constructor with max number of arguments, based on dependencies: $ctor" }
        val arguments = arrayOfNulls<Any>(ctor.parameterCount)
        ctor.parameterTypes.forEachIndexed { i, parameterType ->
            arguments[i] = dependencies[parameterType]
        }
        ctor.trySetAccessible()
        log.trace { "Invoking constructor with arguments " + arguments.contentDeepToString() }
        val subject = ctor.newInstance(*arguments)
        log.debug { "New subject instance created" }
        val notUsedDependencies = dependencies.filter { !ctor.parameterTypes.contains(it.key) }
        log.trace { "Processing dependencies that have not been used in constructor" }
        notUsedDependencies.forEach { (type, dep) ->
            Arrays.stream(subjectClass.declaredFields)
                .filter { it.type == type }
                .findFirst()
                .ifPresentOrElse({
                    it.trySetAccessible()
                    if (it[subject] == null) {
                        log.trace { "Setting unset field ${it.name} to value $dep" }
                        it[subject] = dep
                    }
                }, {
                    Arrays.stream(subjectClass.methods)
                        .filter { it.name.startsWith("set") }
                        .filter { it.parameterCount == 1 }
                        .filter { it.parameterTypes[0] == type }
                        .findFirst()
                        .ifPresent {
                            val getterName = it.name.replace("set", "get")
                            try {
                                val getter = subjectClass.getMethod(getterName)
                                if (getter.invoke(subject) == null) {
                                    log.trace { "Setting unset property with setter ${it.name} to value $dep" }
                                    it.trySetAccessible()
                                    it.invoke(subject, dep)
                                }
                            } catch (_: NoSuchMethodException) {
                                // set anyway if no getter
                                log.trace { "Setting write-only property with setter ${it.name} to value $dep" }
                                it.trySetAccessible()
                                it.invoke(subject, dep)
                            }
                        }
                })
        }
        return subject
    }
}
