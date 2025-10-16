package no.nav.arbeidsgiver.min_side.userinfo

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.plugins.di.*
import no.nav.arbeidsgiver.min_side.FakeApi
import no.nav.arbeidsgiver.min_side.FakeApplication
import no.nav.arbeidsgiver.min_side.fakeToken
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilganger
import no.nav.arbeidsgiver.min_side.services.digisyfo.DigisyfoService
import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.skyscreamer.jsonassert.JSONAssert.assertEquals

class UserInfoServiceTest {
    companion object {
        @RegisterExtension
        val app = FakeApplication(
            addDatabase = true,
        ) {
            dependencies {
                provide<UserInfoService>(UserInfoService::class)
                provide<AltinnService> { Mockito.mock<AltinnService>() }
                provide<DigisyfoService> { Mockito.mock<DigisyfoService>() }
                provide<RefusjonStatusService> { Mockito.mock<RefusjonStatusService>() }
            }
        }

        @RegisterExtension
        val fakeApi = FakeApi()
    }

    @Test
    fun `returnerer organisasjoner og rettigheter for innlogget bruker`() = app.runTest {
        val token = fakeToken("42")
        `when`(app.getDependency<AltinnService>().hentAltinnTilganger(token)).thenReturn(
            AltinnTilganger(
                isError = false,
                hierarki = listOf(
                    AltinnTilganger.AltinnTilgang(
                        orgnr = "1",
                        altinn3Tilganger = setOf(),
                        altinn2Tilganger = setOf(),
                        underenheter = listOf(
                            AltinnTilganger.AltinnTilgang(
                                orgnr = "10",
                                altinn3Tilganger = setOf(),
                                altinn2Tilganger = setOf("3403:1"),
                                underenheter = listOf(),
                                navn = "underenhet",
                                organisasjonsform = "BEDR"
                            )
                        ),
                        navn = "overenhet",
                        organisasjonsform = "AS"
                    ),
                ),
                orgNrTilTilganger = mapOf("10" to setOf("3403:1")),
                tilgangTilOrgNr = mapOf("3403:1" to setOf("10"))
            )
        )
        `when`(app.getDependency<DigisyfoService>().hentVirksomheterOgSykmeldte("42")).thenReturn(
            listOf(
                DigisyfoService.VirksomhetOgAntallSykmeldte(
                    navn = "overenhet",
                    orgnr = "1",
                    organisasjonsform = "AS",
                    antallSykmeldte = 0,
                    orgnrOverenhet = null,
                    underenheter = mutableListOf(
                        DigisyfoService.VirksomhetOgAntallSykmeldte(
                            navn = "underenhet",
                            orgnr = "10",
                            organisasjonsform = "BEDR",
                            antallSykmeldte = 0,
                            orgnrOverenhet = "1",
                            underenheter = mutableListOf(),
                        ),
                    )
                ),
                DigisyfoService.VirksomhetOgAntallSykmeldte(
                    navn = "overenhet",
                    orgnr = "2",
                    organisasjonsform = "AS",
                    antallSykmeldte = 0,
                    orgnrOverenhet = null,
                    underenheter = mutableListOf(
                        DigisyfoService.VirksomhetOgAntallSykmeldte(
                            navn = "underenhet",
                            orgnr = "20",
                            organisasjonsform = "BEDR",
                            antallSykmeldte = 1,
                            orgnrOverenhet = "2",
                            underenheter = mutableListOf(),
                        ),
                    )
                )
            )
        )
        `when`(app.getDependency<RefusjonStatusService>().statusoversikt(token)).thenReturn(
            listOf(
                RefusjonStatusService.Statusoversikt(
                    "314",
                    mapOf(
                        "ny" to 1,
                        "gammel" to 2
                    )
                ),
                RefusjonStatusService.Statusoversikt(
                    "315",
                    mapOf(
                        "ny" to 2,
                        "gammel" to 1
                    )
                ),
            )
        )

        client.get("ditt-nav-arbeidsgiver-api/api/userInfo/v3") {
            bearerAuth(token)
        }.let {
            assert(it.status == HttpStatusCode.OK)
            assertEquals(
                """
                    {
                      "altinnError": false,
                      "digisyfoError": false,
                      "organisasjoner": [
                        {
                          "orgnr": "1",
                          "navn": "overenhet",
                          "organisasjonsform": "AS",
                          "altinn3Tilganger": [],
                          "altinn2Tilganger": [],
                          "underenheter": [
                            {
                              "orgnr": "10",
                              "navn": "underenhet",
                              "organisasjonsform": "BEDR",
                              "altinn3Tilganger": [],
                              "altinn2Tilganger": [
                                "3403:1"
                              ],
                              "underenheter": []
                            }
                          ]
                        }
                      ],
                      "tilganger": {
                        "3403:1": [
                          "10"
                        ]
                      },
                      "digisyfoOrganisasjoner": [
                        {
                          "orgnr": "1",
                          "navn": "overenhet",
                          "organisasjonsform": "AS",
                          "antallSykmeldte": 0,
                          "underenheter": [
                            {
                              "orgnr": "10",
                              "navn": "underenhet",
                              "organisasjonsform": "BEDR",
                              "antallSykmeldte": 0,
                              "underenheter": []
                            }
                          ]
                        },
                        {
                          "orgnr": "2",
                          "navn": "overenhet",
                          "organisasjonsform": "AS",
                          "antallSykmeldte": 0,
                          "underenheter": [
                            {
                              "orgnr": "20",
                              "navn": "underenhet",
                              "organisasjonsform": "BEDR",
                              "antallSykmeldte": 1,
                              "underenheter": []
                            }
                          ]
                        }
                      ],
                      "refusjoner": [
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
                            "ny": 2,
                            "gammel": 1
                          },
                          "tilgang": true
                        }
                      ]
                    }
                    """,
                it.bodyAsText(), true
            )
        }
    }
}
