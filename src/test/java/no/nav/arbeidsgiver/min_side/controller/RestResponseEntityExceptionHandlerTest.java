package no.nav.arbeidsgiver.min_side.controller;

import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.request.WebRequest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@EnableMockOAuth2Server
class RestResponseEntityExceptionHandlerTest {
    @Autowired
    RestResponseEntityExceptionHandler restResponseEntityExceptionHandler;

    @Test
    void loggerMedSirkulærReferanseFraInit() {
        Exception circular = new Exception("foo");
        Exception e2 = new Exception(circular);
        circular.initCause(e2);

        assertDoesNotThrow(() -> {
            restResponseEntityExceptionHandler.handleInternalError(
                    new RuntimeException("wat", circular),
                    mock(WebRequest.class)
            );
        });
    }

    @Test
    void loggerMedSirkulærReferanseFraSupressed() {
        Exception circular = new Exception("foo");
        Exception e2 = new Exception(circular);
        circular.addSuppressed(e2);

        assertDoesNotThrow(() -> {
            restResponseEntityExceptionHandler.handleInternalError(
                    new RuntimeException("wat", circular),
                    mock(WebRequest.class)
            );
        });
    }
}