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
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilganger
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerService
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerServiceImpl
import no.nav.arbeidsgiver.min_side.services.altinn.AccessPackageArea
import no.nav.arbeidsgiver.min_side.services.altinn.AccessPackageMetadata
import no.nav.arbeidsgiver.min_side.services.altinn.LocalizedText
import no.nav.arbeidsgiver.min_side.services.altinn.RessursMetadata
import no.nav.arbeidsgiver.min_side.services.altinn.RessursMetadataResponse
import no.nav.arbeidsgiver.min_side.services.altinn.RessursRegistryRessurs
import no.nav.arbeidsgiver.min_side.services.altinn.RolleMetadata
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.ktor.server.testing.*
import org.skyscreamer.jsonassert.JSONAssert
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AltinnTilgangerApiTest {

    private val defaultDependencies: DependencyRegistry.(ApplicationTestBuilder) -> Unit = {
        provide<TokenXTokenIntrospector> {
            MockTokenIntrospector {
                if (it == "faketoken") mockIntrospectionResponse.withPid("42") else null
            }
        }
        provide<TokenXTokenExchanger> { successTokenXTokenExchanger }
        provide<AltinnTilgangerService>(AltinnTilgangerServiceImpl::class)
    }

    private val defaultApp: suspend Application.() -> Unit = {
        ktorConfig()
        configureTokenXAuth()
        configureAltinnTilgangerRoutes()
    }

    @Test
    fun `returnerer hierarki med enriched altinn3Tilganger og roller`() = runTestApplication(
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
        dependenciesCfg = defaultDependencies,
        applicationCfg = defaultApp,
    ) {
        val response = client.post("ditt-nav-arbeidsgiver-api/api/altinn-tilganger") {
            bearerAuth("faketoken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        JSONAssert.assertEquals(
            """
                {
                  "hierarki": [
                    {
                      "altinn3Tilganger": [
                        {
                          "ressursId": "nav_test_ressurs",
                          "erEnkeltrettighet": null
                        }
                      ],
                      "roller": [
                        {
                          "kode": "DAGL",
                          "visningsnavn": "DAGL"
                        }
                      ],
                      "underenheter": [
                        {
                          "altinn3Tilganger": [
                            {
                              "ressursId": "nav_test_ressurs",
                              "erEnkeltrettighet": null
                            }
                          ],
                          "roller": [
                            {
                              "kode": "DAGL",
                              "visningsnavn": "DAGL"
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
    fun `berikter roller med navn og beskrivelse fra rollemetadata`() = runTestApplication(
        externalServicesCfg = {
            mockAltinnTilganger(
                tilgangerResponse = AltinnTilgangerMock.medTilganger(
                    orgnr = "123456789",
                    rolle = "DAGL",
                ),
                rolesMetadataResponse = mapOf(
                    "dagl" to RolleMetadata(
                        name = "Daglig leder (fra Altinn)",
                        description = "Personen som er ansvarlig for den daglige driften.",
                    ),
                ),
            )
        },
        dependenciesCfg = defaultDependencies,
        applicationCfg = defaultApp,
    ) {
        val response = client.post("ditt-nav-arbeidsgiver-api/api/altinn-tilganger") {
            bearerAuth("faketoken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        JSONAssert.assertEquals(
            """
                {
                  "hierarki": [
                    {
                      "roller": [
                        {
                          "kode": "DAGL",
                          "visningsnavn": "Daglig leder (fra Altinn)",
                          "beskrivelse": "Personen som er ansvarlig for den daglige driften."
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
    fun `roller bruker rollekode som visningsnavn naar rollemetadata mangler`() = runTestApplication(
        externalServicesCfg = {
            mockAltinnTilganger(
                AltinnTilgangerMock.medTilganger(
                    orgnr = "123456789",
                    rolle = "DAGL",
                )
                // ingen rolesMetadataResponse → tom map (default)
            )
        },
        dependenciesCfg = defaultDependencies,
        applicationCfg = defaultApp,
    ) {
        val response = client.post("ditt-nav-arbeidsgiver-api/api/altinn-tilganger") {
            bearerAuth("faketoken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        JSONAssert.assertEquals(
            """
                {
                  "hierarki": [
                    {
                      "roller": [
                        {
                          "kode": "DAGL",
                          "visningsnavn": "DAGL"
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
    fun `berikter altinn3Tilganger med navn og beskrivelse fra ressursmetadata`() = runTestApplication(
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
                ),
            )
        },
        dependenciesCfg = defaultDependencies,
        applicationCfg = defaultApp,
    ) {
        val response = client.post("ditt-nav-arbeidsgiver-api/api/altinn-tilganger") {
            bearerAuth("faketoken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        JSONAssert.assertEquals(
            """
                {
                  "hierarki": [
                    {
                      "altinn3Tilganger": [
                        {
                          "ressursId": "nav_permittering_test",
                          "navn": { "nb": "Permitteringsmelding", "en": "Layoff notice" },
                          "beskrivelse": { "nb": "Rettighet til innsyn" }
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
    fun `beregner delegertViaRoller case-insensitivt`() = runTestApplication(
        externalServicesCfg = {
            mockAltinnTilganger(
                tilgangerResponse = AltinnTilgangerMock.medTilganger(
                    orgnr = "123456789",
                    ressurs = "nav_permittering_test",
                    rolle = "DAGL",
                ),
                ressursMetadataResponse = mapOf(
                    "nav_permittering_test" to RessursMetadata(
                        metadata = RessursRegistryRessurs(identifier = "nav_permittering_test"),
                        grantedByRoles = listOf("dagl"),
                        grantedByAccessPackages = emptyList(),
                    ),
                ),
            )
        },
        dependenciesCfg = defaultDependencies,
        applicationCfg = defaultApp,
    ) {
        val response = client.post("ditt-nav-arbeidsgiver-api/api/altinn-tilganger") {
            bearerAuth("faketoken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        JSONAssert.assertEquals(
            """
                {
                  "hierarki": [
                    {
                      "altinn3Tilganger": [
                        {
                          "ressursId": "nav_permittering_test",
                          "delegertViaRoller": [{ "kode": "DAGL", "visningsnavn": "DAGL" }],
                          "delegertViaTilgangspakker": [],
                          "erEnkeltrettighet": false
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
    fun `beregner delegertViaTilgangspakker`() = runTestApplication(
        externalServicesCfg = {
            mockAltinnTilganger(
                tilgangerResponse = AltinnTilgangerMock.medTilganger(
                    *arrayOf(
                        AltinnTilganger.AltinnTilgang(
                            navn = "Mor AS",
                            orgnr = "111111111",
                            organisasjonsform = "AS",
                            altinn2Tilganger = emptySet(),
                            altinn3Tilganger = setOf("nav_permittering_test"),
                            roller = emptySet(),
                            tilgangspakker = setOf("regnskapsforer-lonn"),
                            underenheter = emptyList(),
                        )
                    )
                ),
                ressursMetadataResponse = mapOf(
                    "nav_permittering_test" to RessursMetadata(
                        metadata = RessursRegistryRessurs(identifier = "nav_permittering_test"),
                        grantedByRoles = emptyList(),
                        grantedByAccessPackages = listOf("regnskapsforer-lonn"),
                    ),
                ),
                accessPackagesMetadataResponse = mapOf(
                    "regnskapsforer-lonn" to AccessPackageMetadata(name = "Regnskapsfører lønn"),
                ),
            )
        },
        dependenciesCfg = defaultDependencies,
        applicationCfg = defaultApp,
    ) {
        val response = client.post("ditt-nav-arbeidsgiver-api/api/altinn-tilganger") {
            bearerAuth("faketoken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        JSONAssert.assertEquals(
            """
                {
                  "hierarki": [
                    {
                      "altinn3Tilganger": [
                        {
                          "ressursId": "nav_permittering_test",
                          "delegertViaRoller": [],
                          "delegertViaTilgangspakker": [{ "id": "regnskapsforer-lonn", "navn": "Regnskapsfører lønn" }],
                          "erEnkeltrettighet": false
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
    fun `erEnkeltrettighet er true naar tilgangen ikke kommer fra rolle eller tilgangspakke`() = runTestApplication(
        externalServicesCfg = {
            mockAltinnTilganger(
                tilgangerResponse = AltinnTilgangerMock.medTilganger(
                    orgnr = "123456789",
                    ressurs = "nav_permittering_test",
                    // ingen rolle eller tilgangspakke
                ),
                ressursMetadataResponse = mapOf(
                    "nav_permittering_test" to RessursMetadata(
                        metadata = RessursRegistryRessurs(identifier = "nav_permittering_test"),
                        grantedByRoles = listOf("dagl"),
                        grantedByAccessPackages = listOf("regnskapsforer-lonn"),
                    ),
                ),
            )
        },
        dependenciesCfg = defaultDependencies,
        applicationCfg = defaultApp,
    ) {
        val response = client.post("ditt-nav-arbeidsgiver-api/api/altinn-tilganger") {
            bearerAuth("faketoken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<AltinnTilgangerResponse>()
        val tilgang = body.hierarki.first().altinn3Tilganger.first { it.ressursId == "nav_permittering_test" }
        assertEquals(true, tilgang.erEnkeltrettighet)
        assertTrue(tilgang.delegertViaRoller.isEmpty())
        assertTrue(tilgang.delegertViaTilgangspakker.isEmpty())
    }

    @Test
    fun `erEnkeltrettighet er null naar ressursmetadata mangler`() = runTestApplication(
        externalServicesCfg = {
            mockAltinnTilganger(
                tilgangerResponse = AltinnTilgangerMock.medTilganger(
                    orgnr = "123456789",
                    ressurs = "nav_test_ressurs",
                )
                // ingen ressursMetadataResponse => tom map (default)
            )
        },
        dependenciesCfg = defaultDependencies,
        applicationCfg = defaultApp,
    ) {
        val response = client.post("ditt-nav-arbeidsgiver-api/api/altinn-tilganger") {
            bearerAuth("faketoken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val tilgang = response.body<AltinnTilgangerResponse>()
            .hierarki.first().altinn3Tilganger.first { it.ressursId == "nav_test_ressurs" }
        assertNull(tilgang.erEnkeltrettighet)
        assertNull(tilgang.navn)
        assertNull(tilgang.beskrivelse)
    }

    @Test
    fun `filtrerer ut altinn3-tilganger som ikke starter med nav_`() = runTestApplication(
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
        dependenciesCfg = defaultDependencies,
        applicationCfg = defaultApp,
    ) {
        val response = client.post("ditt-nav-arbeidsgiver-api/api/altinn-tilganger") {
            bearerAuth("faketoken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        JSONAssert.assertEquals(
            """
                {
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
    fun `berikter tilgangspakker med navn, beskrivelse og area`() = runTestApplication(
        externalServicesCfg = {
            mockAltinnTilganger(
                tilgangerResponse = AltinnTilgangerMock.medTilganger(
                    *arrayOf(
                        AltinnTilganger.AltinnTilgang(
                            navn = "Mor AS",
                            orgnr = "111111111",
                            organisasjonsform = "AS",
                            altinn2Tilganger = emptySet(),
                            altinn3Tilganger = emptySet(),
                            roller = emptySet(),
                            tilgangspakker = emptySet(),
                            underenheter = listOf(
                                AltinnTilganger.AltinnTilgang(
                                    navn = "Datter AS",
                                    orgnr = "222222222",
                                    organisasjonsform = "BEDR",
                                    altinn2Tilganger = emptySet(),
                                    altinn3Tilganger = emptySet(),
                                    roller = emptySet(),
                                    tilgangspakker = setOf("regnskapsforer-lonn"),
                                    underenheter = emptyList(),
                                )
                            ),
                        )
                    )
                ),
                accessPackagesMetadataResponse = mapOf(
                    "regnskapsforer-lonn" to AccessPackageMetadata(
                        name = "Regnskapsfører lønn",
                        description = "Denne fullmakten gir tilgang til lønnstjenester for regnskapsførere.",
                        area = AccessPackageArea(
                            urn = "accesspackage:area:skatt_avgift_regnskap_og_toll",
                            name = "Skatt, avgift, regnskap og toll",
                            description = "Tilgangspakker knyttet til skatt, avgift, regnskap og toll.",
                        ),
                    ),
                    "annen-pakke" to AccessPackageMetadata(name = "Annen pakke"),
                ),
            )
        },
        dependenciesCfg = defaultDependencies,
        applicationCfg = defaultApp,
    ) {
        val response = client.post("ditt-nav-arbeidsgiver-api/api/altinn-tilganger") {
            bearerAuth("faketoken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        JSONAssert.assertEquals(
            """
                {
                  "hierarki": [
                    {
                      "tilgangspakker": [],
                      "underenheter": [
                        {
                          "tilgangspakker": [
                            {
                              "id": "regnskapsforer-lonn",
                              "navn": "Regnskapsfører lønn",
                              "beskrivelse": "Denne fullmakten gir tilgang til lønnstjenester for regnskapsførere.",
                              "area": {
                                "urn": "accesspackage:area:skatt_avgift_regnskap_og_toll",
                                "name": "Skatt, avgift, regnskap og toll",
                                "description": "Tilgangspakker knyttet til skatt, avgift, regnskap og toll."
                              }
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
        val datterTilgangspakker = response.body<AltinnTilgangerResponse>()
            .hierarki.first().underenheter.first().tilgangspakker
        assertEquals(1, datterTilgangspakker.size)
    }

    @Test
    fun `tilgangspakke faar id som navn-fallback naar metadata mangler`() = runTestApplication(
        externalServicesCfg = {
            mockAltinnTilganger(
                tilgangerResponse = AltinnTilgangerMock.medTilganger(
                    *arrayOf(
                        AltinnTilganger.AltinnTilgang(
                            navn = "Bedrift AS",
                            orgnr = "123456789",
                            organisasjonsform = "BEDR",
                            altinn2Tilganger = emptySet(),
                            altinn3Tilganger = emptySet(),
                            roller = emptySet(),
                            tilgangspakker = setOf("ukjent-pakke"),
                            underenheter = emptyList(),
                        )
                    )
                )
                // ingen accessPackagesMetadataResponse
            )
        },
        dependenciesCfg = defaultDependencies,
        applicationCfg = defaultApp,
    ) {
        val response = client.post("ditt-nav-arbeidsgiver-api/api/altinn-tilganger") {
            bearerAuth("faketoken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val pakke = response.body<AltinnTilgangerResponse>()
            .hierarki.first().tilgangspakker.first()
        assertEquals("ukjent-pakke", pakke.id)
        assertEquals("ukjent-pakke", pakke.navn)
        assertNull(pakke.beskrivelse)
        assertNull(pakke.area)
    }

    @Test
    fun `returnerer hierarki uten metadata naar metadata-kall feiler`() = runTestApplication(
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
                    override suspend fun hentRessursMetadata(): RessursMetadataResponse =
                        throw Exception("metadata utilgjengelig")
                    override suspend fun harTilgang(orgnr: String, tjeneste: String, token: String) = false
                    override suspend fun harOrganisasjon(orgnr: String, token: String) = false
                    override suspend fun harRolle(orgnr: String, rolle: String, token: String) = false
                }
            }
        },
        applicationCfg = defaultApp,
    ) {
        val response = client.post("ditt-nav-arbeidsgiver-api/api/altinn-tilganger") {
            bearerAuth("faketoken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<AltinnTilgangerResponse>()
        val tilgang = body.hierarki.first().altinn3Tilganger.first { it.ressursId == "nav_test_ressurs" }
        assertNull(tilgang.navn)
        assertNull(tilgang.beskrivelse)
        assertNull(tilgang.erEnkeltrettighet)
    }
}
