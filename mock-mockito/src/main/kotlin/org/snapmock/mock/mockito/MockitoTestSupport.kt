package org.snapmock.mock.mockito

import org.mockito.Mockito
import org.mockito.kotlin.KStubbing
import org.mockito.kotlin.doReturn
import org.snapmock.core.Source
import org.snapmock.core.TestSupport

object MockitoTestSupport {

    @JvmStatic
    fun depThr(exceptionType: Class<Throwable>, exceptionMessage: String?): Throwable {
        return try {
            if (exceptionMessage != null) {
                exceptionType.getConstructor(Throwable::class.java).newInstance(exceptionMessage)
            } else {
                exceptionType.getConstructor().newInstance()
            }
        } catch (e: NoSuchMethodException) {
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
