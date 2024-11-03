package org.snapmock.mock.mockito

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.KStubbing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import kotlin.test.assertEquals

class ThrowingExceptionTest {

    @Test
    fun throwMockedException() {
        val exception = Mockito.mock(IllegalArgumentException::class.java).apply {
            KStubbing<IllegalArgumentException>(this).apply {
                on { message } doReturn "message"
            }
        }
        val mocked = Mockito.mock(TestClass::class.java).apply {
            KStubbing<TestClass>(this).apply {
                on { test() } doThrow exception
            }
        }
        val e = assertThrows<IllegalArgumentException> {
            mocked.test()
        }
        assertEquals("message", e.message)
    }

}

class TestClass {

    fun test() = "String"

}
