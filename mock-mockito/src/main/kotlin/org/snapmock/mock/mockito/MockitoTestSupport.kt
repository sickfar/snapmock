package org.snapmock.mock.mockito

import io.github.oshai.kotlinlogging.KotlinLogging
import org.mockito.Mockito
import org.mockito.kotlin.KStubbing
import org.mockito.kotlin.doReturn
import org.snapmock.core.Source
import org.snapmock.core.TestSupport

private val log = KotlinLogging.logger {}

object MockitoTestSupport {

    @JvmStatic
    fun depThr(exceptionType: Class<Throwable>, exceptionMessage: String?): Throwable {
        return try {
            if (exceptionMessage != null) {
                log.trace { "Building exception with message argument constructor" }
                exceptionType.getConstructor(Throwable::class.java).newInstance(exceptionMessage)
            } else {
                log.trace { "Building exception with no arguments constructor" }
                exceptionType.getConstructor().newInstance()
            }
        } catch (e: NoSuchMethodException) {
            log.trace { "Cannot build exception with constructor, so exception will be mocked" }
            Mockito.mock(exceptionType).apply {
                KStubbing(this).apply {
                    on {
                        message
                    } doReturn exceptionMessage
                }
            }
        }
    }

    @JvmStatic
    fun depThr(source: Source, depIndex: Int): Throwable {
        val exceptionType = TestSupport.depThrClass<Throwable>(source, depIndex)
        val exceptionMessage = TestSupport.depThrMess(source, depIndex)
        return depThr(exceptionType, exceptionMessage)
    }

}
