package no.nav.arbeidsgiver.min_side.controller

import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.web.context.request.WebRequest

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.flyway.enabled=false"
    ],
)
@EnableMockOAuth2Server
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