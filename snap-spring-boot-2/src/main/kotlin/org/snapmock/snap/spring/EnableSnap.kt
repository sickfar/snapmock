package org.snapmock.snap.spring

import org.snapmock.core.Snap
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

/**
 * Enable support for taking snapshots of
 * beans and bean methods annotated [Snap] [@Snap]
 *
 * @since 1.0.0
 * @see Snap
 */
@Configuration
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(SnapConfiguration::class, SpringDataJpaSnapConfiguration::class)
annotation class EnableSnap
