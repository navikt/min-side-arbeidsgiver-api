package no.nav.arbeidsgiver.min_side

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.arbeidsgiver.min_side.infrastruktur.MockTokenIntrospector
import no.nav.arbeidsgiver.min_side.infrastruktur.TokenXTokenExchanger
import no.nav.arbeidsgiver.min_side.infrastruktur.TokenXTokenIntrospector
import no.nav.arbeidsgiver.min_side.infrastruktur.configureTokenXAuth
import no.nav.arbeidsgiver.min_side.infrastruktur.mockIntrospectionResponse
import no.nav.arbeidsgiver.min_side.infrastruktur.runTestApplication
import no.nav.arbeidsgiver.min_side.infrastruktur.successTokenXTokenExchanger
import no.nav.arbeidsgiver.min_side.infrastruktur.withPid
import no.nav.arbeidsgiver.min_side.ktorConfig
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerService
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerServiceImpl
import no.nav.arbeidsgiver.min_side.services.altinn.LocalizedText
import no.nav.arbeidsgiver.min_side.services.altinn.RessursMetadata
import no.nav.arbeidsgiver.min_side.services.altinn.RessursRegistryRessurs
import org.skyscreamer.jsonassert.JSONAssert
import kotlin.test.Test
import kotlin.test.assertEquals

class AltinnTilgangerApiTest {

    @Test
    fun `returnerer altinn-tilganger for innlogget bruker`() = runTestApplication(
        externalServicesCfg = {
            mockAltinnTilganger(
                AltinnTilgangerMock.medTilganger(
                    orgnr = "123456789",
                    tjeneste = "3403:1",
                    ressurs = "nav_test_ressurs",
                    rolle = "DAGL"
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
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()
            configureAltinnTilgangerRoutes()
        }
    ) {
        val response = client.post("ditt-nav-arbeidsgiver-api/api/altinn-tilganger") {
            bearerAuth("faketoken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            AltinnTilgangerResponse.from(
                AltinnTilgangerMock.medTilganger(
                    orgnr = "123456789",
                    tjeneste = "3403:1",
                    ressurs = "nav_test_ressurs",
                    rolle = "DAGL"
                )
            ),
            response.body<AltinnTilgangerResponse>()
        )
        JSONAssert.assertEquals(
            """
                {
                  "ressursMetadata": {},
                  "hierarki": [
                    {
                      "altinn3Tilganger": ["nav_test_ressurs"],
                      "roller": [
                        {
                          "kode": "DAGL",
                          "visningsnavn": "Daglig leder"
                        }
                      ],
                      "underenheter": [
                        {
                          "altinn3Tilganger": ["nav_test_ressurs"],
                          "roller": [
                            {
                              "kode": "DAGL",
                              "visningsnavn": "Daglig leder"
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
            """.trimIndent(),
            response.bodyAsText(),
            false
        )
    }

    @Test
    fun `berikter respons med ressursmetadata for brukerens nav_-ressurser`() = runTestApplication(
        externalServicesCfg = {
            mockAltinnTilganger(
                tilgangerResponse = AltinnTilgangerMock.medTilganger(
                    orgnr = "123456789",
                    ressurs = "nav_permittering_test",
                ),
                ressursMetadataResponse = mapOf(
                    "nav_permittering_test" to RessursMetadata(
                        metadata = RessursRegistryRessurs(
                            identifier = "nav_permittering_test",
                            title = LocalizedText(nb = "Permitteringsmelding", nn = null, en = "Layoff notice"),
                            rightDescription = LocalizedText(nb = "Rettighet til innsyn", nn = null, en = null),
                            resourceType = "GenericAccessResource",
                            status = "Completed",
                            delegable = true,
                        ),
                        grantedByRoles = listOf("dagl", "lede"),
                        grantedByAccessPackages = listOf("regnskapsforer-lonn"),
                    ),
                    "nav_annen_ressurs" to RessursMetadata(
                        metadata = RessursRegistryRessurs(identifier = "nav_annen_ressurs"),
                        grantedByRoles = emptyList(),
                        grantedByAccessPackages = emptyList(),
                    ),
                ),
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
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()
            configureAltinnTilgangerRoutes()
        }
    ) {
        val response = client.post("ditt-nav-arbeidsgiver-api/api/altinn-tilganger") {
            bearerAuth("faketoken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        JSONAssert.assertEquals(
            """
                {
                  "ressursMetadata": {
                    "nav_permittering_test": {
                      "metadata": {
                        "identifier": "nav_permittering_test",
                        "title": { "nb": "Permitteringsmelding", "en": "Layoff notice" },
                        "rightDescription": { "nb": "Rettighet til innsyn" },
                        "resourceType": "GenericAccessResource",
                        "status": "Completed",
                        "delegable": true
                      },
                      "grantedByRoles": ["dagl", "lede"],
                      "grantedByAccessPackages": ["regnskapsforer-lonn"]
                    }
                  }
                }
            """.trimIndent(),
            response.bodyAsText(),
            false
        )
    }

    @Test
    fun `filtrerer ressursmetadata til kun brukerens tilgjengelige ressurser`() = runTestApplication(
        externalServicesCfg = {
            mockAltinnTilganger(
                tilgangerResponse = AltinnTilgangerMock.medTilganger(
                    orgnr = "123456789",
                    ressurs = "nav_ressurs_a",
                ),
                ressursMetadataResponse = mapOf(
                    "nav_ressurs_a" to RessursMetadata(
                        metadata = RessursRegistryRessurs(identifier = "nav_ressurs_a"),
                        grantedByRoles = listOf("dagl"),
                        grantedByAccessPackages = emptyList(),
                    ),
                    "nav_ressurs_b" to RessursMetadata(
                        metadata = RessursRegistryRessurs(identifier = "nav_ressurs_b"),
                        grantedByRoles = emptyList(),
                        grantedByAccessPackages = emptyList(),
                    ),
                ),
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
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()
            configureAltinnTilgangerRoutes()
        }
    ) {
        val response = client.post("ditt-nav-arbeidsgiver-api/api/altinn-tilganger") {
            bearerAuth("faketoken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<AltinnTilgangerResponse>()
        assertEquals(setOf("nav_ressurs_a"), body.ressursMetadata.keys)
    }

    @Test
    fun `filtrerer ut altinn2-tilganger og altinn3-tilganger som ikke starter med nav_`() = runTestApplication(
        externalServicesCfg = {
            mockAltinnTilganger(
                AltinnTilgangerMock.medTilganger(
                    orgnr = "123456789",
                    tjeneste = "3403:1",
                    ressurs = "ikke_nav_ressurs",
                    rolle = "DAGL"
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
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()
            configureAltinnTilgangerRoutes()
        }
    ) {
        val response = client.post("ditt-nav-arbeidsgiver-api/api/altinn-tilganger") {
            bearerAuth("faketoken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        JSONAssert.assertEquals(
            """
                {
                  "ressursMetadata": {},
                  "hierarki": [
                    {
                      "altinn3Tilganger": [],
                      "underenheter": [
                        {
                          "altinn3Tilganger": []
                        }
                      ]
                    }
                  ]
                }
            """.trimIndent(),
            response.bodyAsText(),
            false
        )
    }

    @Test
    fun `returnerer tomt ressursMetadata dersom metadata-kall feiler`() = runTestApplication(
        externalServicesCfg = {
            mockAltinnTilganger(
                AltinnTilgangerMock.medTilganger(
                    orgnr = "123456789",
                    ressurs = "nav_test_ressurs",
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
            provide<AltinnTilgangerService> {
                object : AltinnTilgangerService {
                    private val tilganger = AltinnTilgangerMock.medTilganger(
                        orgnr = "123456789",
                        ressurs = "nav_test_ressurs",
                    )

                    override suspend fun hentAltinnTilganger(token: String) = tilganger
                    override suspend fun hentRessursMetadata(): Map<String, no.nav.arbeidsgiver.min_side.services.altinn.RessursMetadata> =
                        throw Exception("metadata utilgjengelig")
                    override suspend fun harTilgang(orgnr: String, tjeneste: String, token: String) = false
                    override suspend fun harOrganisasjon(orgnr: String, token: String) = false
                    override suspend fun harRolle(orgnr: String, rolle: String, token: String) = false
                }
            }
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()
            configureAltinnTilgangerRoutes()
        }
    ) {
        val response = client.post("ditt-nav-arbeidsgiver-api/api/altinn-tilganger") {
            bearerAuth("faketoken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<AltinnTilgangerResponse>()
        assertEquals(emptyMap(), body.ressursMetadata)
    }
}
