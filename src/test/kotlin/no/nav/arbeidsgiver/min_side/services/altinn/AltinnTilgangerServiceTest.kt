package no.nav.arbeidsgiver.min_side.services.altinn

import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder
import no.nav.arbeidsgiver.min_side.services.tokenExchange.TokenExchangeClient
import no.nav.arbeidsgiver.min_side.services.tokenExchange.TokenXToken
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpMethod.POST
import org.springframework.http.MediaType.APPLICATION_JSON
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

    @MockBean
    lateinit var tokenXClient: TokenExchangeClient

    @MockBean
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

        val organisasjoner = altinnTilgangerService.hentOrganisasjoner()

        assertTrue(organisasjoner.size == 2)

        val parent = organisasjoner.first({ it.organizationNumber == "810825472" })
        val underenhet = organisasjoner.first({ it.organizationNumber == "910825496" })

        assertTrue(parent.name == "Arbeids- og Velferdsetaten")
        assertTrue(underenhet.name == "SLEMMESTAD OG STAVERN REGNSKAP")
        assertTrue(parent.organizationForm == "ORGL")
        assertTrue(underenhet.organizationForm == "BEDR")
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

    companion object {
        @JvmStatic
        fun `basert på tilgagner testdata`(): List<Arguments> {
            return listOf<Arguments>(
                Arguments.of("4936", "1", 1),
                Arguments.of("4936", "2", 0),
            )
        }
    }
}


private val altinnTilgangerResponse = """
    {
      "isError": false,
      "hierarki": [
        {
          "orgNr": "810825472",
          "altinn3Tilganger": [],
          "altinn2Tilganger": [],
          "underenheter": [
            {
              "orgNr": "910825496",
              "altinn3Tilganger": [
                "test-fager"
              ],
              "altinn2Tilganger": [
                "4936:1"
              ],
              "underenheter": [],
              "name": "SLEMMESTAD OG STAVERN REGNSKAP",
              "organizationForm": "BEDR"
            }
          ],
          "name": "Arbeids- og Velferdsetaten",
          "organizationForm": "ORGL"
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
