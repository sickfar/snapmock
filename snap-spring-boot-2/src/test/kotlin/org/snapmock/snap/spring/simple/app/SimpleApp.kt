package org.snapmock.snap.spring.simple.app

import org.snapmock.snap.spring.EnableSnap
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.PropertySource

@EnableSnap
@SpringBootApplication(scanBasePackages = ["org.snapmock.snap.spring.simple.app"])
@PropertySource("classpath:simple/application.properties")
open class SimpleApp {

}
