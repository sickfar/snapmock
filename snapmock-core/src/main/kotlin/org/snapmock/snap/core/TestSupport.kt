package org.snapmock.snap.core

import com.fasterxml.jackson.databind.ObjectMapper

object TestSupport {

    private var objectMapperCustomizer: SnapMockObjectMapperCustomizer? = null

    private val objectMapper: ObjectMapper by lazy { objectMapper(objectMapperCustomizer) }

    @JvmStatic
    fun setObjectMapperCustomizer(customizer: SnapMockObjectMapperCustomizer) {
        objectMapperCustomizer = customizer
    }

    private val reader: SnapReader by lazy { SnapReader(objectMapper) }

}
