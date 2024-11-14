package org.snapmock.snap.spring.data

import org.snapmock.snap.spring.EnableSnap
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.PropertySource

@EnableSnap
@SpringBootApplication(scanBasePackages = ["org.snapmock.snap.spring.data"])
@PropertySource("classpath:data/application.properties")
class SpringDataApp
