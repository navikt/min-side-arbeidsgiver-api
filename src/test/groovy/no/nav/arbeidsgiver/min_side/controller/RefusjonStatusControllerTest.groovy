package no.nav.arbeidsgiver.min_side.controller

import groovy.json.JsonSlurper
import no.nav.arbeidsgiver.min_side.models.Organisasjon
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusRepository
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(
        value = RefusjonStatusController,
        properties = [
            "server.servlet.context-path=/",
            "tokensupport.enabled=false",
        ]
)
class RefusjonStatusControllerTest extends Specification {

    @SpringBean
    AuthenticatedUserHolder authenticatedUserHolder = Mock()

    @SpringBean
    AltinnService altinnService = Mock()

    @SpringBean
    RefusjonStatusRepository refusjonStatusRepository = Mock()

    @Autowired
    MockMvc mockMvc

    def jsonSlurper = new JsonSlurper()

    def orgnr1 = "314"
    def orgnr2 = "315"

    def "Statusoversikt"() {
        given:
        authenticatedUserHolder.getFnr() >> "42"
        altinnService.hentOrganisasjonerBasertPaRettigheter(_ as String, _ as String, _ as String) >> [
                organisasjon(orgnr1),
                organisasjon(orgnr2),
        ]
        refusjonStatusRepository.statusoversikt([orgnr1, orgnr2]) >> [
                statusoversikt(orgnr1, ["ny": 1, "gammel": 2]),
                statusoversikt(orgnr2, ["ny": 3, "gammel": 4]),
        ]

        expect:
        def jsonResponse = mockMvc.perform(
                get("/api/refusjon_status").accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn().response.contentAsString
        jsonSlurper.parseText(jsonResponse) == jsonSlurper.parseText("""
        [
          {
            "virksomhetsnummer": "314",
            "statusoversikt": {
              "ny": 1,
              "gammel": 2
            },
            "tilgang": true
          },
          {
            "virksomhetsnummer": "315",
            "statusoversikt": {
              "ny": 3,
              "gammel": 4
            },
            "tilgang": true
          }
        ]
        """)
    }

    private RefusjonStatusRepository.Statusoversikt statusoversikt(
            String orgnr,
            Map<String, Integer> status
    ) {
        new RefusjonStatusRepository.Statusoversikt(orgnr, status)
    }

    private Organisasjon organisasjon(String orgnr) {
        Organisasjon.builder().organizationNumber(orgnr).build()
    }
}
