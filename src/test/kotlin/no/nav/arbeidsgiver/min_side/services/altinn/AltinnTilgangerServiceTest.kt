package no.nav.arbeidsgiver.min_side.services.altinn

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import no.nav.arbeidsgiver.min_side.infrastruktur.TokenResponse
import no.nav.arbeidsgiver.min_side.infrastruktur.TokenXTokenExchanger
import no.nav.arbeidsgiver.min_side.infrastruktur.resolve
import no.nav.arbeidsgiver.min_side.infrastruktur.runTestApplication
import no.nav.arbeidsgiver.min_side.mockAltinnTilganger
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilganger.AltinnTilgang
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class AltinnTilgangerServiceTest {

    @Test
    fun `henter organisasjoner fra altinn tilganger proxy`() = runTestApplication(
        externalServicesCfg = {
            mockAltinnTilganger {
                require(call.request.authorization() == "Bearer user-token-x") {
                    // verify token is exchanged and forwarded?
                    "Ugyldig token i kall til altinn tilganger proxy"
                }
                call.respondText(altinnTilgangerResponse, ContentType.Application.Json)
            }
        },
        dependenciesCfg = {
            provide<TokenXTokenExchanger> { object : TokenXTokenExchanger {
                override suspend fun exchange(
                    target: String, userToken: String
                ) = TokenResponse.Success("$userToken-x", 3600)
            } }
            provide<AltinnTilgangerService>(AltinnTilgangerServiceImpl::class)
        }
    ) {
        val altinnService = resolve<AltinnTilgangerService>()
        val tilganger = altinnService.hentAltinnTilganger("user-token")
        assertFalse(tilganger.isError)
        assertEquals(tilganger.hierarki.size, 1)
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
        assertTrue(tilganger.tilgangTilOrgNr.containsKey("4936:1"))
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
