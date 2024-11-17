package org.snapmock.snap.spring.simple

import org.snapmock.snap.spring.EnableSnap
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.context.annotation.PropertySource

@EnableSnap
@SpringBootApplication(
    scanBasePackages = ["org.snapmock.snap.spring.simple"],
    // because we have data source plugged in, but I want keep it simple here
    exclude = [DataSourceAutoConfiguration::class]
)
@PropertySource("classpath:simple/application.properties")
class SimpleApp
