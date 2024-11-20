package io.github.sickfar.snapmock.core

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule

/**
 * Customize Jackson ObjectMapper implementing this interface and passing it to [objectMapper] function
 *
 * This functional interface implementation will receive ObjectMapper instance with default SnapMock configuration as an argument
 *
 * @since 1.0.0
 * @author Roman Aksenenko
 */
fun interface SnapMockObjectMapperCustomizer {
    /**
     * Customize ObjectMapper
     * @param objectMapper ObjectMapper instance with default SnapMock configuration
     */
    fun customize(objectMapper: ObjectMapper)
}

/**
 * Create a Jackson ObjectMapper to serialize and deserialize snapshots
 * @param customizer ObjectMapper customizer which receives ObjectMapper instance as an argument
 * @return Configured Jackson ObjectMapper
 *
 * @see SnapMockObjectMapperCustomizer
 * @since 1.0.0
 * @author Roman Aksenenko
 */
fun objectMapper(customizer: SnapMockObjectMapperCustomizer? = null): ObjectMapper {
    val objectMapper = jacksonObjectMapper()
        .configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false)
        .configure(SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL, true)
        .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(JavaTimeModule())
        .registerModule(Jdk8Module())
        .registerModule(kotlinModule())
    customizer?.customize(objectMapper)
    return objectMapper
}
