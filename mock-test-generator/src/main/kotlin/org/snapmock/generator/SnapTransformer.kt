package org.snapmock.generator

import org.snapmock.generator.data.SnapMockTest
import org.snapmock.snap.core.SnapDataSource

class SnapTransformer(
    private val mockFrameworkGenerator: MockFrameworkGenerator,
    private val assertFrameworkGenerator: AssertFrameworkGenerator
) {

    fun transform(snapDataSource: SnapDataSource): SnapMockTest {
        val (source, snap) = snapDataSource
        val testClassAnnotations = mockFrameworkGenerator.generateTestClassAnnotations() + assertFrameworkGenerator.generateTestClassAnnotations()
        val testMethodAnnotations = mockFrameworkGenerator.generateTestMethodAnnotations() + assertFrameworkGenerator.generateTestMethodAttributes()
        val subject = mockFrameworkGenerator.generateSubject(snap.main, source)
        val dependencies = snap.dependencies.mapIndexed { index, it -> mockFrameworkGenerator.generateMockingDependency(it, source, index) }.toSet()
        val mocks = snap.dependencies.mapIndexed { index, depSnap -> mockFrameworkGenerator.generateMock(depSnap, source, index) }
        val assertions = assertFrameworkGenerator.generateAssertions(snap.main, source)
        return SnapMockTest(
            testClassAnnotations = testClassAnnotations,
            testMethodAnnotations = testMethodAnnotations,
            subject = subject,
            subjectMethod = snap.main.methodName,
            dependencies = dependencies,
            mocks = mocks,
            assertions = assertions
        )
    }

}
