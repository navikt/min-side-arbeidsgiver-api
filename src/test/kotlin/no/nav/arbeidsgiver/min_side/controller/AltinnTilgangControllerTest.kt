package no.nav.arbeidsgiver.min_side.controller

import no.nav.arbeidsgiver.min_side.clients.altinn.AltinnTilgangssøknadClient
import no.nav.arbeidsgiver.min_side.models.AltinnTilgangssøknad
import no.nav.arbeidsgiver.min_side.models.AltinnTilgangssøknadsskjema
import no.nav.arbeidsgiver.min_side.models.Organisasjon
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(
    value = [AltinnTilgangController::class],
    properties = ["server.servlet.context-path=/", "tokensupport.enabled=false"]
)
class AltinnTilgangControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var authenticatedUserHolder: AuthenticatedUserHolder

    @MockBean
    lateinit var altinnTilgangssøknadClient: AltinnTilgangssøknadClient

    @MockBean
    lateinit var altinnService: AltinnService

    @Test
    fun mineSøknaderOmTilgang() {
        `when`(authenticatedUserHolder.fnr).thenReturn("42")
        val altinnTilgangssøknad = AltinnTilgangssøknad()
        altinnTilgangssøknad.orgnr = "314"
        altinnTilgangssøknad.serviceCode = "13337"
        altinnTilgangssøknad.serviceEdition = 3
        altinnTilgangssøknad.status = "Created"
        altinnTilgangssøknad.createdDateTime = "now"
        altinnTilgangssøknad.lastChangedDateTime = "whenever"
        altinnTilgangssøknad.submitUrl = "https://yolo.com"
        `when`(altinnTilgangssøknadClient.hentSøknader("42")).thenReturn(listOf(altinnTilgangssøknad))

        val jsonResponse = mockMvc
            .perform(get("/api/altinn-tilgangssoknad").accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk)
            .andReturn().response.contentAsString
        JSONAssert.assertEquals(
            """
            [
              {
                "orgnr": "314",
                "serviceCode": "13337",
                "serviceEdition": 3,
                "status": "Created", 
                "createdDateTime": "now", 
                "lastChangedDateTime": "whenever",
                "submitUrl": "https://yolo.com"
              }
            ]
            """,
            jsonResponse,
            true
        )
    }

    @Test
    fun sendSøknadOmTilgang() {
        val skjema = AltinnTilgangssøknadsskjema()
        skjema.orgnr = "314"
        skjema.redirectUrl = "https://yolo.it"
        skjema.serviceCode = AltinnTilgangController.våreTjenester.first().left
        skjema.serviceEdition = AltinnTilgangController.våreTjenester.first().right.toInt()

        val søknad = AltinnTilgangssøknad()
        søknad.orgnr = "314"
        søknad.serviceCode = "13337"
        søknad.serviceEdition = 3
        søknad.status = "Created"
        søknad.createdDateTime = "now"
        søknad.lastChangedDateTime = "whenever"
        søknad.submitUrl = "https://yolo.com"

        `when`(authenticatedUserHolder.fnr).thenReturn("42")
        `when`(altinnService.hentOrganisasjoner("42")).thenReturn(listOf(Organisasjon(null, null, null, "314", null, null)))
        `when`(altinnTilgangssøknadClient.sendSøknad("42", skjema)).thenReturn(søknad)

        val jsonResponse = mockMvc
            .perform(
                post("/api/altinn-tilgangssoknad")
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "orgnr": "${skjema.orgnr}",
                            "redirectUrl": "${skjema.redirectUrl}",
                            "serviceCode": "${skjema.serviceCode}",
                            "serviceEdition": ${skjema.serviceEdition}
                        }
                    """)
            )
            .andDo(print())
            .andExpect(status().isOk)
            .andReturn().response.contentAsString
        JSONAssert.assertEquals(
            """
              {
                "orgnr": "${søknad.orgnr}",
                "serviceCode": "${søknad.serviceCode}",
                "serviceEdition": ${søknad.serviceEdition},
                "status": "${søknad.status}", 
                "createdDateTime": "${søknad.createdDateTime}", 
                "lastChangedDateTime": "${søknad.lastChangedDateTime}",
                "submitUrl": "${søknad.submitUrl}"
              }
            """,
            jsonResponse,
            true
        )
    }
}