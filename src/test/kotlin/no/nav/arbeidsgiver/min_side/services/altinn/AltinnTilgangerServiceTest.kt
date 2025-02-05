package no.nav.arbeidsgiver.min_side.services.altinn

import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilganger.AltinnTilgang
import no.nav.arbeidsgiver.min_side.services.tokenExchange.TokenExchangeClient
import no.nav.arbeidsgiver.min_side.services.tokenExchange.TokenXToken
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.http.HttpMethod.POST
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.*
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess


@RestClientTest(
    AltinnService::class,
)
class AltinnTilgangerServiceTest {
    @Autowired
    lateinit var altinnServer: MockRestServiceServer

    @Autowired
    lateinit var altinnTilgangerService: AltinnService

    @MockitoBean
    lateinit var tokenXClient: TokenExchangeClient

    @MockitoBean
    lateinit var authenticatedUserHolder: AuthenticatedUserHolder

    @Test
    fun `henter organisasjoner fra altinn tilganger proxy`() {
        `when`(tokenXClient.exchange(anyString(), anyString()))
            .thenReturn(TokenXToken(access_token = "access_token2"))

        `when`(authenticatedUserHolder.token).thenReturn("access_token1")

        altinnServer.expect(requestTo("http://arbeidsgiver-altinn-tilganger/altinn-tilganger"))
            .andExpect(method(POST))
            .andExpect(header("Authorization", "Bearer access_token2"))
            .andRespond(
                withSuccess(altinnTilgangerResponse, APPLICATION_JSON)
            )

        val tilganger = altinnTilgangerService.hentAltinnTilganger()

        assertFalse(tilganger.isError)
        assertTrue(tilganger.hierarki.size == 1)
        assertEquals(
            AltinnTilganger(
                isError = false,
                hierarki = listOf(
                    AltinnTilgang(
                        orgnr = "810825472",
                        navn = "Arbeids- og Velferdsetaten",
                        organisasjonsform = "ORGL",
                        altinn3Tilganger = emptySet(),
                        altinn2Tilganger = emptySet(),
                        underenheter = listOf(
                            AltinnTilgang(
                                orgnr = "910825496",
                                navn = "SLEMMESTAD OG STAVERN REGNSKAP",
                                organisasjonsform = "BEDR",
                                altinn3Tilganger = setOf("test-fager"),
                                altinn2Tilganger = setOf("4936:1"),
                                underenheter = emptyList(),
                            )
                        )
                    )
                ),
                orgNrTilTilganger = mapOf(
                    "910825496" to setOf("test-fager", "4936:1")
                ),
                tilgangTilOrgNr = mapOf(
                    "test-fager" to setOf("910825496"),
                    "4936:1" to setOf("910825496")
                )
            ),
            tilganger,
        )
    }

    @Test
    fun `henter altinn tilganger basert på rettigheter`() {
        `when`(tokenXClient.exchange(anyString(), anyString()))
            .thenReturn(TokenXToken(access_token = "access_token2"))

        `when`(authenticatedUserHolder.token).thenReturn("access_token1")

        altinnServer.expect(requestTo("http://arbeidsgiver-altinn-tilganger/altinn-tilganger"))
            .andExpect(method(POST))
            .andExpect(header("Authorization", "Bearer access_token2"))
            .andRespond(
                withSuccess(altinnTilgangerResponse, APPLICATION_JSON)
            )

        val organisasjoner = altinnTilgangerService.hentAltinnTilganger()
        assertTrue(organisasjoner.tilgangTilOrgNr.containsKey("4936:1"))
    }
}


//language=JSON
private val altinnTilgangerResponse = """
    {
      "isError": false,
      "hierarki": [
        {
          "orgnr": "810825472",
          "navn": "Arbeids- og Velferdsetaten",
          "organisasjonsform": "ORGL",
          "altinn3Tilganger": [],
          "altinn2Tilganger": [],
          "underenheter": [
            {
              "orgnr": "910825496",
              "navn": "SLEMMESTAD OG STAVERN REGNSKAP",
              "organisasjonsform": "BEDR",
              "altinn3Tilganger": [
                "test-fager"
              ],
              "altinn2Tilganger": [
                "4936:1"
              ],
              "underenheter": []
            }
          ]
        }
      ],
      "orgNrTilTilganger": {
        "910825496": [
          "test-fager",
          "4936:1"
        ]
      },
      "tilgangTilOrgNr": {
        "test-fager": [
          "910825496"
        ],
        "4936:1": [
          "910825496"
        ]
      }
    }
""".trimIndent()
