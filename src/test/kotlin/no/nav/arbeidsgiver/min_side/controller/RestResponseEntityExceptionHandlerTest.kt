package no.nav.arbeidsgiver.min_side.controller

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.web.context.request.WebRequest

@MockBean(JwtDecoder::class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.flyway.enabled=false"
    ],
)
class RestResponseEntityExceptionHandlerTest {

    @Autowired
    lateinit var restResponseEntityExceptionHandler: RestResponseEntityExceptionHandler

    @Test
    fun loggerMedSirkulærReferanseFraInit() {
        val circular = Exception("foo")
        val e2 = Exception(circular)
        circular.initCause(e2)
        Assertions.assertDoesNotThrow {
            restResponseEntityExceptionHandler.handleInternalError(
                RuntimeException("wat", circular),
                Mockito.mock(WebRequest::class.java)
            )
        }
    }

    @Test
    fun loggerMedSirkulærReferanseFraSupressed() {
        val circular = Exception("foo")
        val e2 = Exception(circular)
        circular.addSuppressed(e2)
        Assertions.assertDoesNotThrow {
            restResponseEntityExceptionHandler.handleInternalError(
                RuntimeException("wat", circular),
                Mockito.mock(WebRequest::class.java)
            )
        }
    }
}