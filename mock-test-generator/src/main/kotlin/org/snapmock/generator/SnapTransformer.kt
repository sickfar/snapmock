package org.snapmock.generator

import org.snapmock.generator.data.SnapMockTest
import org.snapmock.snap.core.SnapData

class SnapTransformer(
    val testFrameworkGenerator: TestFrameworkGenerator,
    val mockFrameworkGenerator: MockFrameworkGenerator,
    val assertFrameworkGenerator: AssertFrameworkGenerator
) {

    fun transform(snap: SnapData): SnapMockTest {
        TODO()
    }

}
