package io.github.sickfar.snapmock.spring.simple.hello

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import io.github.sickfar.snapmock.core.Snap
import io.github.sickfar.snapmock.core.SnapDepFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.OutputStream

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

    fun getNull(): HelloData? {
        return null
    }

    fun getDepNull(): HelloData? {
        return helloRepository.getNull(null)
    }

    fun getStreaming(): StreamingResponseBody {
        return object: StreamingResponseBody {
            override fun writeTo(os: OutputStream) {
                ObjectMapper().registerModule(kotlinModule())
                    .writeValue(os, HelloData(helloRepository.message))
            }
        }
    }

}
