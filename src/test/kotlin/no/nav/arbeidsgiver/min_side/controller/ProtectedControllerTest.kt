package no.nav.arbeidsgiver.min_side.controller

import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusRepository
import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@MockBean(
    MultiIssuerConfiguration::class,
    AuthenticatedUserHolder::class,
    AltinnService::class,
    RefusjonStatusRepository::class
)
@WebMvcTest(
    value = [RefusjonStatusController::class],
    properties = ["server.servlet.context-path=/"],
)
class ProtectedControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `TokenSupport er Default Enabled`() {
        mockMvc
            .perform(MockMvcRequestBuilders.get("/api/refusjon_status").accept(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)

    }
}