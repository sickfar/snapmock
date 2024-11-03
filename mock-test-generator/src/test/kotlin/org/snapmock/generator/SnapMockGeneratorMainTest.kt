package org.snapmock.generator

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.snapmock.snap.core.StreamSource
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
                javaClass.getResourceAsStream("/snap/HelloController_get_2024_11_01T20_52_33_762752Z.json")!!
            })
        )
    }

}
