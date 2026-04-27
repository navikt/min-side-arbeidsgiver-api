package no.nav.arbeidsgiver.min_side.services.altinn

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import no.nav.arbeidsgiver.min_side.infrastruktur.TokenResponse
import no.nav.arbeidsgiver.min_side.infrastruktur.TokenXTokenExchanger
import no.nav.arbeidsgiver.min_side.infrastruktur.resolve
import no.nav.arbeidsgiver.min_side.infrastruktur.runTestApplication
import no.nav.arbeidsgiver.min_side.mockAltinnTilganger
import no.nav.arbeidsgiver.min_side.mockAltinnTilgangerMedMetadataHandler
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilganger.AltinnTilgang
import no.nav.arbeidsgiver.min_side.services.altinn.LocalizedText
import no.nav.arbeidsgiver.min_side.services.altinn.RessursMetadata
import no.nav.arbeidsgiver.min_side.services.altinn.RessursMetadataResponse
import no.nav.arbeidsgiver.min_side.services.altinn.RessursRegistryRessurs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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
                        roller = emptySet(),
                        underenheter = listOf(
                            AltinnTilgang(
                                orgnr = "910825496",
                                navn = "SLEMMESTAD OG STAVERN REGNSKAP",
                                organisasjonsform = "BEDR",
                                altinn3Tilganger = setOf("test-fager"),
                                altinn2Tilganger = setOf("4936:1"),
                                underenheter = emptyList(),
                                roller = emptySet(),
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
                ),
            ),
            tilganger,
        )
        assertTrue(tilganger.tilgangTilOrgNr.containsKey("4936:1"))
    }

    @Test
    fun `harRolle returnerer true når bruker har rollen på orgnr`() = runTestApplication(
        externalServicesCfg = {
            mockAltinnTilganger {
                call.respondText(altinnTilgangerMedRollerResponse, ContentType.Application.Json)
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
        assertTrue(altinnService.harRolle("910825496", "DAGL", "user-token"))
        assertTrue(altinnService.harRolle("910825496", "HADM", "user-token"))
        assertTrue(altinnService.harRolle("810825472", "BEST", "user-token"))
    }

    @Test
    fun `harRolle returnerer false når bruker ikke har rollen`() = runTestApplication(
        externalServicesCfg = {
            mockAltinnTilganger {
                call.respondText(altinnTilgangerMedRollerResponse, ContentType.Application.Json)
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
        assertFalse(altinnService.harRolle("910825496", "BEST", "user-token"))
        assertFalse(altinnService.harRolle("810825472", "DAGL", "user-token"))
        assertFalse(altinnService.harRolle("999999999", "DAGL", "user-token"))
    }

    @Test
    fun `harRolle returnerer false når ingen roller finnes`() = runTestApplication(
        externalServicesCfg = {
            mockAltinnTilganger {
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
        assertFalse(altinnService.harRolle("910825496", "DAGL", "user-token"))
        assertFalse(altinnService.harRolle("810825472", "DAGL", "user-token"))
    }

    @Test
    fun `hentRessursMetadata henter metadata uten auth-header`() = runTestApplication(
        externalServicesCfg = {
            mockAltinnTilgangerMedMetadataHandler(
                handler = { call.respondText(altinnTilgangerResponse, ContentType.Application.Json) },
                ressursMetadataHandler = {
                    assertNull(call.request.authorization(), "resource-metadata skal ikke ha Authorization-header")
                    call.respond(
                        RessursMetadataResponse(
                            resources = mapOf(
                                "nav_test" to RessursMetadata(
                                    metadata = RessursRegistryRessurs(
                                        identifier = "nav_test",
                                        title = LocalizedText(nb = "Test ressurs"),
                                    ),
                                    grantedByRoles = listOf("dagl"),
                                    grantedByAccessPackages = emptyList(),
                                )
                            )
                        )
                    )
                },
            )
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
        val metadata = altinnService.hentRessursMetadata()
        assertEquals(1, metadata.size)
        assertEquals("nav_test", metadata["nav_test"]?.metadata?.identifier)
        assertEquals(listOf("dagl"), metadata["nav_test"]?.grantedByRoles)
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
          "roller": [],
          "underenheter": [
            {
              "orgnr": "910825496",
              "navn": "SLEMMESTAD OG STAVERN REGNSKAP",
              "organisasjonsform": "BEDR",
              "roller": [],
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

//language=JSON
private val altinnTilgangerMedRollerResponse = """
    {
      "isError": false,
      "hierarki": [
        {
          "orgnr": "810825472",
          "navn": "Arbeids- og Velferdsetaten",
          "organisasjonsform": "ORGL",
          "altinn3Tilganger": [],
          "altinn2Tilganger": [],
          "roller": ["BEST"],
          "underenheter": [
            {
              "orgnr": "910825496",
              "navn": "SLEMMESTAD OG STAVERN REGNSKAP",
              "organisasjonsform": "BEDR",
              "roller": ["DAGL", "HADM"],
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
