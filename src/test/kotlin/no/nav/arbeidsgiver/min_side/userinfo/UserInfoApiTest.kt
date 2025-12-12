package no.nav.arbeidsgiver.min_side.userinfo

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.arbeidsgiver.min_side.AltinnTilgangerMock
import no.nav.arbeidsgiver.min_side.AltinnTilgangerMock.medTilgang
import no.nav.arbeidsgiver.min_side.configureUserInfoRoutes
import no.nav.arbeidsgiver.min_side.infrastruktur.*
import no.nav.arbeidsgiver.min_side.ktorConfig
import no.nav.arbeidsgiver.min_side.mockAltinnTilganger
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerService
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerServiceImpl
import no.nav.arbeidsgiver.min_side.services.digisyfo.*
import no.nav.arbeidsgiver.min_side.services.ereg.EregClient
import no.nav.arbeidsgiver.min_side.services.ereg.EregClientImpl
import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusRepository
import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusService
import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusService.Companion.RESSURS_ID
import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusServiceImpl
import org.skyscreamer.jsonassert.JSONAssert
import java.util.UUID.randomUUID
import kotlin.test.Test
import kotlin.test.assertEquals

class UserInfoApiTest {
    @Test
    fun `returnerer organisasjoner og rettigheter for innlogget bruker`() = runTestApplication(
        externalServicesCfg = {
            mockAltinnTilganger(
                AltinnTilgangerMock.medTilganger(
                    orgnr = "10",
                    tjeneste = "3403:1"
                )
            )
        },
        dependenciesCfg = {
            provide<TokenXTokenIntrospector> {
                MockTokenIntrospector {
                    if (it == "faketoken") mockIntrospectionResponse.withPid("42") else null
                }
            }
            provide<TokenXTokenExchanger> { successTokenXTokenExchanger }
            provide<AltinnTilgangerService>(AltinnTilgangerServiceImpl::class)

            // TODO: populate database with refusjon status records instead of mocking service
            provide<RefusjonStatusService> {
                object : RefusjonStatusService {
                    override suspend fun statusoversikt(token: String): List<RefusjonStatusService.Statusoversikt> {
                        require(token == "faketoken") { "Ukjent token" }

                        return listOf(
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
                    }
                }
            }

            // TODO: populate database with digisyfo data instead of mocking service
            provide<DigisyfoService> {
                object : DigisyfoService {
                    override suspend fun hentVirksomheterOgSykmeldte(fnr: String): List<DigisyfoService.VirksomhetOgAntallSykmeldte> {
                        require(fnr == "42") { "Ukjent fnr" }

                        return listOf(
                            DigisyfoService.VirksomhetOgAntallSykmeldte(
                                navn = "10-parent",
                                orgnr = "10-parent",
                                organisasjonsform = "AS",
                                antallSykmeldte = 0,
                                orgnrOverenhet = null,
                                underenheter = mutableListOf(
                                    DigisyfoService.VirksomhetOgAntallSykmeldte(
                                        navn = "10",
                                        orgnr = "10",
                                        organisasjonsform = "BEDR",
                                        antallSykmeldte = 0,
                                        orgnrOverenhet = "10-parent",
                                        underenheter = mutableListOf(),
                                    ),
                                )
                            ),
                            DigisyfoService.VirksomhetOgAntallSykmeldte(
                                navn = "20-parent",
                                orgnr = "20-parent",
                                organisasjonsform = "AS",
                                antallSykmeldte = 0,
                                orgnrOverenhet = null,
                                underenheter = mutableListOf(
                                    DigisyfoService.VirksomhetOgAntallSykmeldte(
                                        navn = "20",
                                        orgnr = "20",
                                        organisasjonsform = "BEDR",
                                        antallSykmeldte = 1,
                                        orgnrOverenhet = "20-parent",
                                        underenheter = mutableListOf(),
                                    ),
                                )
                            )
                        )
                    }

                }
            }
            provide(UserInfoService::class)
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()
            configureUserInfoRoutes()
        }
    ) {
        client.get("ditt-nav-arbeidsgiver-api/api/userInfo/v3") {
            bearerAuth("faketoken")
        }.let {
            assertEquals(HttpStatusCode.OK, it.status)
            JSONAssert.assertEquals(
                """
                    {
                      "altinnError": false,
                      "digisyfoError": false,
                      "organisasjoner": [
                        {
                          "orgnr": "10-parent",
                          "navn": "10-parent",
                          "organisasjonsform": "AS",
                          "altinn3Tilganger": [],
                          "altinn2Tilganger": [
                            "3403:1"
                          ],
                          "underenheter": [
                            {
                              "orgnr": "10",
                              "navn": "10",
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
                          "10-parent", "10"
                        ]
                      },
                      "digisyfoOrganisasjoner": [
                        {
                          "orgnr": "10-parent",
                          "navn": "10-parent",
                          "organisasjonsform": "AS",
                          "antallSykmeldte": 0,
                          "underenheter": [
                            {
                              "orgnr": "10",
                              "navn": "10",
                              "organisasjonsform": "BEDR",
                              "antallSykmeldte": 0,
                              "underenheter": []
                            }
                          ]
                        },
                        {
                          "orgnr": "20-parent",
                          "navn": "20-parent",
                          "organisasjonsform": "AS",
                          "antallSykmeldte": 0,
                          "underenheter": [
                            {
                              "orgnr": "20",
                              "navn": "20",
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

    @Test
    fun `statusoversikt returnerer riktig innhold`() = runTestApplicationWithDatabase(
        externalServicesCfg = {
            mockAltinnTilganger(
                AltinnTilgangerMock.medTilganger(
                    medTilgang(orgnr = "314", ressurs = RESSURS_ID),
                    medTilgang(orgnr = "315", ressurs = RESSURS_ID)
                )
            )
        },
        dependenciesCfg = {
            provide<TokenXTokenIntrospector> {
                MockTokenIntrospector {
                    if (it == "faketoken") mockIntrospectionResponse.withPid("42") else null
                }
            }
            provide<TokenXTokenExchanger> { successTokenXTokenExchanger }
            provide<AltinnTilgangerService>(AltinnTilgangerServiceImpl::class)
            provide(RefusjonStatusRepository::class)
            provide(RefusjonStatusRecordProcessor::class)
            provide<RefusjonStatusService>(RefusjonStatusServiceImpl::class)
            provide<EregClient>(EregClientImpl::class)
            provide<DigisyfoRepository>(DigisyfoRepositoryImpl::class)
            provide<DigisyfoService>(DigisyfoServiceImpl::class)
            provide(UserInfoService::class)
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()
            configureUserInfoRoutes()
        }
    ) {
        resolve<RefusjonStatusRecordProcessor>().apply {
            processRefusjonStatus("314", "ny")
            processRefusjonStatus("314", "gammel")
            processRefusjonStatus("314", "gammel")
            processRefusjonStatus("315", "ny")
            processRefusjonStatus("315", "ny")
            processRefusjonStatus("315", "gammel")
        }

        client.get("ditt-nav-arbeidsgiver-api/api/userInfo/v3") {
            bearerAuth("faketoken")
        }.also {
            assertEquals(HttpStatusCode.OK, it.status)
            JSONAssert.assertEquals(
                """
                    {
                      refusjoner: [
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
                    """, it.bodyAsText(), false
            )
        }
    }
}

private suspend fun RefusjonStatusRecordProcessor.processRefusjonStatus(vnr: String, status: String) =
    processRecordValue("""
    {
        "refusjonId": "${randomUUID()}",
        "bedriftNr": "$vnr",
        "avtaleId": "${randomUUID()}",
        "status": "$status"
    }
    """)
