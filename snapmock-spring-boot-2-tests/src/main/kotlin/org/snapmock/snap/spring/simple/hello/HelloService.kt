package org.snapmock.snap.spring.simple.hello

import org.snapmock.core.Snap
import org.snapmock.core.SnapDepFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Snap
@Service
class HelloService(
    private val helloRepository: HelloRepository,
    private val factoryByClass: HelloProviderFactoryByClass,
    private val factoryByMethod: HelloProviderFactoryByMethod
) {

    @field:Autowired
    @field:SnapDepFactory
    lateinit var factoryByField: HelloProviderFactoryByField

    @set:Autowired
    @field:SnapDepFactory
    lateinit var factoryBySetter: HelloProviderFactoryBySetter

    fun get(): HelloData {
        return HelloData(helloRepository.message)
    }

    fun post(data: String): HelloData {
        return helloRepository.post(data)
    }

    fun getByFactory(factory: HelloFactory): HelloData {
        return when(factory) {
            HelloFactory.CLASS -> HelloData(factoryByClass.provider.getGreeting())
            HelloFactory.METHOD -> HelloData(factoryByMethod.provider.getGreeting())
            HelloFactory.FIELD -> HelloData(factoryByField.provider.getGreeting())
            HelloFactory.SETTER -> HelloData(factoryBySetter.provider.getGreeting())
        }
    }

}
