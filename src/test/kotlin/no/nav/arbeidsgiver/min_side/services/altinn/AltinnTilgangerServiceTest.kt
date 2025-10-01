package no.nav.arbeidsgiver.min_side.services.altinn

import io.ktor.http.*
import io.ktor.server.plugins.di.*
import io.ktor.server.response.*
import no.nav.arbeidsgiver.min_side.FakeApi
import no.nav.arbeidsgiver.min_side.FakeApplication
import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilganger.AltinnTilgang
import no.nav.arbeidsgiver.min_side.services.tokenExchange.TokenExchangeClient
import no.nav.arbeidsgiver.min_side.services.tokenExchange.TokenXToken
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.`when`

class AltinnTilgangerServiceTest {
    companion object {
        @RegisterExtension
        val app = FakeApplication(
            addDatabase = true,
        ) {
            dependencies {
                provide<AltinnService>(AltinnService::class)
                provide<TokenExchangeClient> { Mockito.mock<TokenExchangeClient>() }
            }
        }

        @RegisterExtension
        val fakeApi = FakeApi()
    }

    @Test
    fun `henter organisasjoner fra altinn tilganger proxy`() = app.runTest {
        val tokenXClient = app.getDependency<TokenExchangeClient>()
        `when`(tokenXClient.exchange(anyString(), anyString()))
            .thenReturn(TokenXToken(access_token = "access_token2"))

        fakeApi.registerStub(
            HttpMethod.Post,
            "/altinn-tilganger"
        ) {
            assertEquals(call.request.headers["Authorization"], "Bearer access_token2")
            call.respondText(
                altinnTilgangerResponse,
                ContentType.Application.Json
            )
        }


        val tilganger = app.getDependency<AltinnService>().hentAltinnTilganger("access_token1")

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
    fun `henter altinn tilganger basert på rettigheter`() = app.runTest {
        val tokenXClient = app.getDependency<TokenExchangeClient>()
        `when`(tokenXClient.exchange(anyString(), anyString()))
            .thenReturn(TokenXToken(access_token = "access_token2"))

        fakeApi.registerStub(
            HttpMethod.Post,
            "/altinn-tilganger"
        ) {
            assertEquals(call.request.headers["Authorization"], "Bearer access_token2")
            call.respondText(
                altinnTilgangerResponse,
                ContentType.Application.Json
            )
        }


        val organisasjoner = app.getDependency<AltinnService>().hentAltinnTilganger("access_token1")
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
