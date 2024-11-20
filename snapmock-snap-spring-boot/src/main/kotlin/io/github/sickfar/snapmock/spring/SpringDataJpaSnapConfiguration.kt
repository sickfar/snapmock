package io.github.sickfar.snapmock.spring

import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module
import com.fasterxml.jackson.datatype.hibernate5.jakarta.Hibernate5JakartaModule
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module
import io.github.sickfar.snapmock.core.SnapMockObjectMapperCustomizer
import org.hibernate.Version
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnClass(name = ["org.hibernate.SessionFactory"])
open class SpringDataJpaSnapConfiguration {

    @Bean
    open fun objectMapperCustomizerForHibernate(): SnapMockObjectMapperCustomizer {
        return SnapMockObjectMapperCustomizer {
            val hibernateVersion = Version.getVersionString()
            if (hibernateVersion.startsWith("5")) {
                if (isJakarta()) {
                    val module = Hibernate5JakartaModule()
                    module.enable(Hibernate5JakartaModule.Feature.FORCE_LAZY_LOADING)
                    module.enable(Hibernate5JakartaModule.Feature.REPLACE_PERSISTENT_COLLECTIONS)
                    module.enable(Hibernate5JakartaModule.Feature.WRITE_MISSING_ENTITIES_AS_NULL)
                    it.registerModule(module)
                } else {
                    val module = Hibernate5Module()
                    module.enable(Hibernate5Module.Feature.FORCE_LAZY_LOADING)
                    module.enable(Hibernate5Module.Feature.REPLACE_PERSISTENT_COLLECTIONS)
                    module.enable(Hibernate5Module.Feature.WRITE_MISSING_ENTITIES_AS_NULL)
                    it.registerModule(module)
                }
            } else if (hibernateVersion.startsWith("6")) {
                val module = Hibernate6Module()
                module.enable(Hibernate6Module.Feature.FORCE_LAZY_LOADING)
                module.enable(Hibernate6Module.Feature.REPLACE_PERSISTENT_COLLECTIONS)
                module.enable(Hibernate6Module.Feature.WRITE_MISSING_ENTITIES_AS_NULL)
                it.registerModule(module)
            }
        }
    }

    private fun isJakarta(): Boolean {
        try {
            Class.forName("jakarta.persistence.Transient", false, javaClass.classLoader)
            return true
        } catch (_: ClassNotFoundException) {
            // it's ok not to have class
            return false
        }
    }

}
