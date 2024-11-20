package io.github.sickfar.snapmock.spring.simple.hello

import io.github.sickfar.snapmock.core.SnapResultPostprocessor
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.OutputStream

@Component
class StreamingResponseBodySnapPostprocessor: SnapResultPostprocessor<StreamingResponseBody> {
    override fun accept(t: StreamingResponseBody) {
        t.writeTo(OutputStream.nullOutputStream())
    }
}
