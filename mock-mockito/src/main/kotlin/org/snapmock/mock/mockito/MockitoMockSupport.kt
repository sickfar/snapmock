package org.snapmock.mock.mockito

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.kotlin.KStubbing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.snapmock.core.Source
import org.snapmock.core.TestSupport
import java.util.*
import java.util.function.BiConsumer
import java.util.stream.Collectors

fun interface AssertionBiConsumer<E, A>: BiConsumer<E, A> {
    override fun accept(expected: E, actual: A)
}

object MockitoMockSupport {

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun configureMocks(test: Any, source: Source) {
        val snap = TestSupport.snap(source)
        val mockObjects = Arrays.stream(test.javaClass.declaredFields)
            .filter { it.isAnnotationPresent(Mock::class.java) }
            .peek { it.trySetAccessible() }
            .collect(Collectors.toMap({ it.type }, { it.get(test) }))
        snap.dependencies.forEachIndexed { depIndex, dependency ->
            val dependencyClass = Class.forName(dependency.className)
            val mockObject = mockObjects[dependencyClass]!!
            val parameterTypes = dependency.parameterTypes.map { Class.forName(it) }.toTypedArray()
            val mockMethod = dependencyClass.getMethod(dependency.methodName, *parameterTypes)
            if (dependency.exceptionType != null) {
                val exceptionType: Class<Throwable> = Class.forName(dependency.exceptionType) as Class<Throwable>
                val exceptionMessage = dependency.exceptionMessage
                if (exceptionMessage != null) {
                    val exception = MockitoTestSupport.depThr(exceptionType, exceptionMessage)
                    KStubbing(mockObject).apply {
                        on {
                            mockMethod.invoke(this, mapDepArgs(dependency.arguments.size, source, depIndex))
                        } doThrow exception
                    }
                } else {
                    KStubbing(mockObject).apply {
                        on {
                            mockMethod.invoke(this, mapDepArgs(dependency.arguments.size, source, depIndex))
                        }.thenThrow(exceptionType)
                    }
                }
            } else {
                KStubbing(mockObject).apply {
                    on {
                        mockMethod.invoke(this, mapDepArgs(dependency.arguments.size, source, depIndex))
                    } doReturn TestSupport.depResult(source, depIndex)
                }
            }
        }
        snap.factories.forEachIndexed { depIndex, factory ->
            val dependencyClass = Class.forName(factory.className)
            val mockObject = mockObjects[dependencyClass]!!
            val parameterTypes = factory.parameterTypes.map { Class.forName(it) }.toTypedArray()
            val mockMethod = dependencyClass.getMethod(factory.methodName, *parameterTypes)
            val resultClass = Class.forName(factory.returnType)
            KStubbing(mockObject).apply {
                on {
                    mockMethod.invoke(this, mapFactoryArgs(factory.arguments.size, source, depIndex))
                } doReturn mockObjects[resultClass]!!
            }
        }
    }

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun <E, A> doSnapshotTest(
        test: Any,
        source: Source,
        expectedConverter: java.util.function.Function<Any?, E>?,
        asserts: AssertionBiConsumer<E, A>
    ) {
        val snap = TestSupport.snap(source)
        configureMocks(test, source)
        val subjectClass = Class.forName(snap.main.className)
        val subject = Arrays.stream(test.javaClass.declaredFields)
            .filter { it.isAnnotationPresent(InjectMocks::class.java) }
            .filter { it.type == subjectClass }
            .peek { it.trySetAccessible() }
            .findFirst().orElseThrow { IllegalStateException("No field of type ${snap.main.className} annotated @InjectMocks found") }
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

    private fun mapDepArgs(size: Int, source: Source, depIndex: Int) {
        val args = arrayOfNulls<Any>(size)
        for (argIndex in args.indices) {
            val arg = TestSupport.depArg<Any>(source, depIndex, argIndex)
            args[argIndex] = ArgumentMatchers.eq(arg)
        }
    }

    private fun mapFactoryArgs(size: Int, source: Source, depIndex: Int) {
        val args = arrayOfNulls<Any>(size)
        for (argIndex in args.indices) {
            val arg = TestSupport.factArg<Any>(source, depIndex, argIndex)
            args[argIndex] = ArgumentMatchers.eq(arg)
        }
    }
}
