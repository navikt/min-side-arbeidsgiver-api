package no.nav.arbeidsgiver.min_side.controller

import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusRepository
import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration
import org.spockframework.spring.StubBeans
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@StubBeans([
        MultiIssuerConfiguration,
        AuthenticatedUserHolder,
        AltinnService,
        RefusjonStatusRepository
])
@WebMvcTest(
        value = RefusjonStatusController,
        properties = [
                "server.servlet.context-path=/",
        ]
)
class TokenSupportDefaultEnabledTest extends Specification {

    @Autowired
    MockMvc mockMvc

    def "Statusoversikt"() {
        expect:
        mockMvc.perform(
                get("/api/refusjon_status").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
    }

}
