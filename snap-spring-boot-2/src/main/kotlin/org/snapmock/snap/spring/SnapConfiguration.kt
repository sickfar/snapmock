package org.snapmock.snap.spring

import com.fasterxml.jackson.databind.ObjectMapper
import org.snapmock.core.*
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import
import java.nio.file.Path
import java.util.stream.Collectors

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

@Configuration
@EnableConfigurationProperties(SnapConfigurationProperties::class)
@EnableAspectJAutoProxy
@Import(SnapAspect::class)
@ConditionalOnProperty(prefix = "snapmock.snap", value = ["enabled"], havingValue = "true")
open class SnapConfiguration(
    private val properties: SnapConfigurationProperties
) {

    @Bean
    open fun objectMapperHolder(customizers: List<SnapMockObjectMapperCustomizer>): SnapObjectMapperHolder {
        return SnapObjectMapperHolder(objectMapper { mapper ->
            customizers.forEach { it.customize(mapper) }
        })
    }

    @Bean
    open fun snapWriter(objectMapperHolder: SnapObjectMapperHolder): SnapWriter {
        return SnapWriter(properties.directory, objectMapperHolder.objectMapper)
    }

    @Bean
    open fun invocationStorage() = InvocationStorage()

}

@ConfigurationProperties(prefix = "snapmock.snap")
open class SnapConfigurationProperties @ConstructorBinding constructor(
    val enabled: Boolean = false,
    val directory: Path = defaultDirectory(),
    val ignore: SnapConfigurationPropertiesIgnore = SnapConfigurationPropertiesIgnore()
)

open class SnapConfigurationPropertiesIgnore @ConstructorBinding constructor(
    val classes: List<String> = defaultIgnoreClasses()
) {
    val mappedClasses: List<Class<*>> by lazy {
        return@lazy classes.stream()
            .map { safeByName(it) }
            .filter { it != null }
            .map { it!! }
            .collect(Collectors.toList())
    }

    private fun safeByName(className: String): Class<*>? {
        try {
            return Class.forName(className)
        } catch (e: ClassNotFoundException) {
            return null
        }
    }
}

class SnapObjectMapperHolder(
    val objectMapper: ObjectMapper
)
