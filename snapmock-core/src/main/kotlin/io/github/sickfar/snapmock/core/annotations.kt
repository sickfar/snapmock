package io.github.sickfar.snapmock.core

/**
 * Annotate a bean or a bean method to make an invocation snapshot
 *
 * @since 1.0.0
 * @author Roman Aksenenko
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Snap

/**
 * Annotate a bean or a bean method to mark that invocation snapshot cannot or should not be taken
 * because this bean produces another bean (which invocation snapshot should be taken) during initial invocation
 *
 * @since 1.0.0
 * @author Roman Aksenenko
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.FIELD
)
@Retention(AnnotationRetention.RUNTIME)
annotation class SnapDepFactory
