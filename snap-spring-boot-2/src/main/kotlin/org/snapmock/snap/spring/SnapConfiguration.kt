package org.snapmock.snap.spring

import com.fasterxml.jackson.databind.ObjectMapper
import org.snapmock.snap.core.InvocationStorage
import org.snapmock.snap.core.SnapMockObjectMapperCustomizer
import org.snapmock.snap.core.SnapWriter
import org.snapmock.snap.core.objectMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.jvm.optionals.getOrNull

@Configuration
@EnableConfigurationProperties(SnapConfigurationProperties::class)
@EnableAspectJAutoProxy
@Import(SnapAspect::class)
@ConditionalOnProperty(prefix = "snapmock.snap", value = ["enabled"], havingValue = "true")
open class SnapConfiguration(
    private val properties: SnapConfigurationProperties
) {

    @Bean
    open fun objectMapperHolder(customizer: Optional<SnapMockObjectMapperCustomizer>): SnapObjectMapperHolder {
        return SnapObjectMapperHolder(objectMapper(customizer.getOrNull()))
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
    val directory: Path = Paths.get("./snap"),
)

class SnapObjectMapperHolder(
    val objectMapper: ObjectMapper = objectMapper(null)
)
