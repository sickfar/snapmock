package org.snapmock.snap.spring

import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module
import com.fasterxml.jackson.datatype.hibernate5.jakarta.Hibernate5JakartaModule
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module
import org.hibernate.Version
import org.snapmock.core.SnapMockObjectMapperCustomizer
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
                it.registerModule(Hibernate5Module())
            } else if (hibernateVersion.startsWith("5.5") || hibernateVersion.startsWith("5.6")) {
                it.registerModule(Hibernate5JakartaModule())
            } else if (hibernateVersion.startsWith("6")) {
                it.registerModule(Hibernate6Module())
            }
        }
    }

}
