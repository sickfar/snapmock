package org.snapmock.snap.spring

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(SnapConfiguration::class)
annotation class EnableSnap()
