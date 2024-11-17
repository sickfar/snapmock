package io.github.sickfar.snapmock.spring.data

import io.github.sickfar.snapmock.spring.EnableSnap
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.PropertySource

@EnableSnap
@SpringBootApplication(scanBasePackages = ["io.github.sickfar.snapmock.snap.spring.data"])
@PropertySource("classpath:data/application.properties")
class SpringDataApp
