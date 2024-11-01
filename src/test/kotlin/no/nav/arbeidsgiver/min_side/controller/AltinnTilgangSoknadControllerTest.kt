package no.nav.arbeidsgiver.min_side.controller

import no.nav.arbeidsgiver.min_side.clients.altinn.AltinnTilgangssøknadClient
import no.nav.arbeidsgiver.min_side.config.SecurityConfig
import no.nav.arbeidsgiver.min_side.controller.SecurityMockMvcUtil.Companion.jwtWithPid
import no.nav.arbeidsgiver.min_side.models.AltinnTilgangssøknad
import no.nav.arbeidsgiver.min_side.models.AltinnTilgangssøknadsskjema
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.web.client.HttpClientErrorException
import java.nio.charset.Charset

@MockBean(JwtDecoder::class)
@WebMvcTest(
    value = [
        AltinnTilgangSoknadController::class,
        SecurityConfig::class,
        AuthenticatedUserHolder::class,
    ],
    properties = [
        "server.servlet.context-path=/"
    ]
)
class AltinnTilgangSoknadControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var altinnTilgangssøknadClient: AltinnTilgangssøknadClient

    @MockBean
    lateinit var altinnService: AltinnService

    @Test
    fun mineSøknaderOmTilgang() {
        val altinnTilgangssøknad = AltinnTilgangssøknad()
        altinnTilgangssøknad.orgnr = "314"
        altinnTilgangssøknad.serviceCode = "13337"
        altinnTilgangssøknad.serviceEdition = 3
        altinnTilgangssøknad.status = "Created"
        altinnTilgangssøknad.createdDateTime = "now"
        altinnTilgangssøknad.lastChangedDateTime = "whenever"
        altinnTilgangssøknad.submitUrl = "https://yolo.com"
        `when`(altinnTilgangssøknadClient.hentSøknader("42")).thenReturn(listOf(altinnTilgangssøknad))

        val jsonResponse = mockMvc.get("/api/altinn-tilgangssoknad") {
            with(jwtWithPid("42"))
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
        }.andReturn().response.contentAsString

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
        val skjema = AltinnTilgangssøknadsskjema(
            orgnr = "314",
            redirectUrl = "https://yolo.it",
            serviceCode = AltinnTilgangSoknadController.tjenester.first().first,
            serviceEdition = AltinnTilgangSoknadController.tjenester.first().second,
        )
        val søknad = AltinnTilgangssøknad(
            orgnr = "314",
            serviceCode = "13337",
            serviceEdition = 3,
            status = "Created",
            createdDateTime = "now",
            lastChangedDateTime = "whenever",
            submitUrl = "https://yolo.com",
        )

        `when`(altinnService.harOrganisasjon(skjema.orgnr)).thenReturn(true)
        `when`(altinnTilgangssøknadClient.sendSøknad("42", skjema)).thenReturn(søknad)

        val jsonResponse = mockMvc
            .post("/api/altinn-tilgangssoknad") {
                with(jwtWithPid("42"))
                accept = MediaType.APPLICATION_JSON
                contentType = MediaType.APPLICATION_JSON
                content = """
                        {
                            "orgnr": "${skjema.orgnr}",
                            "redirectUrl": "${skjema.redirectUrl}",
                            "serviceCode": "${skjema.serviceCode}",
                            "serviceEdition": ${skjema.serviceEdition}
                        }
                    """
            }.andExpect {
                status { isOk() }
            }.andReturn().response.contentAsString

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

    @Test
    fun sendSøknadOmTilgangSomAlleredeErSøktPåGirBadRequest() {
        val skjema = AltinnTilgangssøknadsskjema(
            orgnr = "314",
            redirectUrl = "https://yolo.it",
            serviceCode = AltinnTilgangSoknadController.tjenester.first().first,
            serviceEdition = AltinnTilgangSoknadController.tjenester.first().second,
        )

        `when`(altinnService.harOrganisasjon("314")).thenReturn(true)
        `when`(altinnTilgangssøknadClient.sendSøknad("42", skjema)).thenThrow(
            HttpClientErrorException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "Bad Request",
                """[{"ErrorCode":"40318","ErrorMessage":"This request for access has already been registered"}]""".encodeToByteArray(),
                Charset.defaultCharset()
            )
        )

        mockMvc
            .post("/api/altinn-tilgangssoknad") {
                with(jwtWithPid("42"))
                accept = MediaType.APPLICATION_JSON
                contentType = MediaType.APPLICATION_JSON
                content = """
                        {
                            "orgnr": "${skjema.orgnr}",
                            "redirectUrl": "${skjema.redirectUrl}",
                            "serviceCode": "${skjema.serviceCode}",
                            "serviceEdition": ${skjema.serviceEdition}
                        }
                    """
            }.andExpect {
                status { isBadRequest() }
            }

    }
}