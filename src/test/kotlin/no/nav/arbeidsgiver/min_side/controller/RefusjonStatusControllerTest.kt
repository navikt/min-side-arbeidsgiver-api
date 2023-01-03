package no.nav.arbeidsgiver.min_side.controller

import no.nav.arbeidsgiver.min_side.models.Organisasjon
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusRepository
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(
    value = [RefusjonStatusController::class],
    properties = ["server.servlet.context-path=/", "tokensupport.enabled=false"]
)
class RefusjonStatusControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var authenticatedUserHolder: AuthenticatedUserHolder

    @MockBean
    lateinit var altinnService: AltinnService

    @MockBean
    lateinit var refusjonStatusRepository: RefusjonStatusRepository

    @Test
    fun Statusoversikt() {
        val orgnr1 = "314"
        val orgnr2 = "315"

        Mockito.`when`(authenticatedUserHolder.fnr).thenReturn("42")
        Mockito.`when`(altinnService.hentOrganisasjonerBasertPaRettigheter(anyString(), anyString(), anyString())).thenReturn(listOf(
            organisasjon(orgnr1),
            organisasjon(orgnr2),
        ))
        Mockito.`when`(refusjonStatusRepository.statusoversikt(listOf(orgnr1, orgnr2))).thenReturn(listOf(
            statusoversikt(orgnr1, mapOf("ny" to 1, "gammel" to 2)),
            statusoversikt(orgnr2, mapOf("ny" to 3, "gammel" to 4)),
        ))

        val jsonResponse = mockMvc
            .perform(get("/api/refusjon_status").accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk)
            .andReturn().response.contentAsString
        JSONAssert.assertEquals(
            """
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
            """,
            jsonResponse,
            true
        )
    }
}

private fun organisasjon(orgnr: String) = Organisasjon(organizationNumber = orgnr)

private fun statusoversikt(orgnr: String, status: Map<String, Int>) =
    RefusjonStatusRepository.Statusoversikt(orgnr, status)