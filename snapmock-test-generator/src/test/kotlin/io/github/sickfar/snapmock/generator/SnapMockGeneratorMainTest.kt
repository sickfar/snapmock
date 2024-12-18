package io.github.sickfar.snapmock.generator

import io.github.sickfar.snapmock.core.StreamSource
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.nio.file.Path

class SnapMockGeneratorMainTest {

    @Disabled
    @Test
    fun testProcessJUnitMockitoHamcrestJavaPerFile() {
        val output = Path.of("./.gen/")
        process(
            MockFramework.MOCKITO,
            TestFramework.JUNIT,
            AssertFramework.HAMCREST,
            Lang.JAVA,
            Mode.PER_SNAP_FILE,
            output,
            listOf(StreamSource {
                javaClass.getResourceAsStream("/snap/HelloService_get_simple.json")!!
            })
        )
    }

    @Disabled
    @Test
    fun testProcessJUnitMockitoHamcrestJavaPerFileWithThrows() {
        val output = Path.of("./.gen/")
        process(
            MockFramework.MOCKITO,
            TestFramework.JUNIT,
            AssertFramework.HAMCREST,
            Lang.JAVA,
            Mode.PER_SNAP_FILE,
            output,
            listOf(StreamSource {
                javaClass.getResourceAsStream("/snap/HelloService_get_throws.json")!!
            })
        )
    }

}
