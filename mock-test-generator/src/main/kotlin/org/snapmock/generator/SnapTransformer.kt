package org.snapmock.generator

import org.snapmock.generator.data.SnapMockTest
import org.snapmock.snap.core.SnapFromSource

class SnapTransformer(
    private val mockFrameworkGenerator: MockFrameworkGenerator,
    private val assertFrameworkGenerator: AssertFrameworkGenerator
) {

    fun transform(snapFromSource: SnapFromSource): SnapMockTest {
        val (source, snap) = snapFromSource
        val statics = mockFrameworkGenerator.generateStatics() + assertFrameworkGenerator.generateStatics()
        val testClassAnnotations = mockFrameworkGenerator.generateTestClassAnnotations() + assertFrameworkGenerator.generateTestClassAnnotations()
        val testMethodAnnotations = mockFrameworkGenerator.generateTestMethodAnnotations() + assertFrameworkGenerator.generateTestMethodAttributes()
        val subject = mockFrameworkGenerator.generateSubject(snap.main, source)
        val dependencies = snap.dependencies.mapIndexed { index, it -> mockFrameworkGenerator.generateMockingDependency(it, source, index) }.toSet()
        val mocks = snap.dependencies.mapIndexed { index, depSnap -> mockFrameworkGenerator.generateMock(depSnap, source, index) }
        val assertion = assertFrameworkGenerator.generateAssertions(snap.main, source)
        return SnapMockTest(
            source = source,
            statics = statics,
            testClassAnnotations = testClassAnnotations,
            testMethodAnnotations = testMethodAnnotations,
            subject = subject,
            subjectMethod = snap.main.methodName,
            dependencies = dependencies,
            mocks = mocks,
            assertion = assertion
        )
    }

}
