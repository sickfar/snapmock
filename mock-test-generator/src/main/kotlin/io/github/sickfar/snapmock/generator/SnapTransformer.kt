package io.github.sickfar.snapmock.generator

import io.github.sickfar.snapmock.core.SnapFromSource
import io.github.sickfar.snapmock.generator.data.SnapMockTest

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
        val dependencies = snap.dependencies.map { mockFrameworkGenerator.generateMockingDependencyDeclaration(source, it.key, it.value) }.toSet()
        // TODO generate factories
        val mocks = snap.dependents.mapIndexed { index, depSnap -> mockFrameworkGenerator.generateMock(depSnap, source, index) }
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
