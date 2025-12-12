package no.nav.arbeidsgiver.min_side.kontostatus

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import no.nav.arbeidsgiver.min_side.AltinnTilgangerMock
import no.nav.arbeidsgiver.min_side.configureKontonummerRoutes
import no.nav.arbeidsgiver.min_side.infrastruktur.*
import no.nav.arbeidsgiver.min_side.ktorConfig
import no.nav.arbeidsgiver.min_side.mockAltinnTilganger
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerService
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerServiceImpl
import no.nav.arbeidsgiver.min_side.services.kontostatus.KontoregisterClient
import no.nav.arbeidsgiver.min_side.services.kontostatus.KontoregisterClientImpl
import no.nav.arbeidsgiver.min_side.services.kontostatus.KontostatusService
import no.nav.arbeidsgiver.min_side.services.kontostatus.kontonummerTilgangTjenesetekode
import org.skyscreamer.jsonassert.JSONAssert
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class KontostatusApiTest {

    @Test
    fun `henter kontonummer fra kontoregister`() = runTestApplication(
        externalServicesCfg = {
            mockKontoregister {
                if (call.parameters["orgnummer"] == "42") {
                    call.respondText(
                        //language=JSON
                        """
                        {
                            "mottaker": "42",
                            "kontonr": "12345678901"
                        }
                        """,
                        ContentType.Application.Json
                    )
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        },
        dependenciesCfg = { builder ->
            provide<TokenXTokenIntrospector> {
                MockTokenIntrospector {
                    if (it == "faketoken") mockIntrospectionResponse.withPid("42") else null
                }
            }
            provide<AzureAdTokenProvider> { successAzureAdTokenProvider }
            provide<KontoregisterClient> {
                KontoregisterClientImpl(
                    tokenProvider = resolve(),
                    httpClient = builder.createClient {
                        expectSuccess = true
                        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                            json(defaultJson)
                        }
                    }
                )
            }
            provide<AltinnTilgangerService> {
                object : AltinnTilgangerService {
                    override suspend fun hentAltinnTilganger(token: String) =
                        TODO("Not yet implemented")

                    override suspend fun harTilgang(orgnr: String, tjeneste: String, token: String) =
                        TODO("Not yet implemented")

                    override suspend fun harOrganisasjon(orgnr: String, token: String) =
                        TODO("Not yet implemented")
                }
            }
            provide(KontostatusService::class)
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()

            configureKontonummerRoutes()
        }
    ) {
        resolve<KontoregisterClient>().hentKontonummer("42").let {
            assertNotNull(it)
            assertEquals("42", it.mottaker)
            assertEquals("12345678901", it.kontonr)
        }

        client.post("ditt-nav-arbeidsgiver-api/api/kontonummerStatus/v1") {
            contentType(ContentType.Application.Json)
            setBody("""{"virksomhetsnummer": "42"}""")
            bearerAuth("faketoken")
        }.let {
            assertEquals(HttpStatusCode.OK, it.status)
            JSONAssert.assertEquals(it.bodyAsText(), """{"status": "OK"}""", true)
        }
    }

    @Test
    fun `finner ikke kontonummer for virksomhet`() = runTestApplication(
        externalServicesCfg = {
            mockKontoregister {
                call.respond(HttpStatusCode.NotFound)
            }
        },
        dependenciesCfg = { builder ->
            provide<TokenXTokenIntrospector> {
                MockTokenIntrospector {
                    if (it == "faketoken") mockIntrospectionResponse.withPid("42") else null
                }
            }
            provide<AzureAdTokenProvider> { successAzureAdTokenProvider }
            provide<KontoregisterClient> {
                KontoregisterClientImpl(
                    tokenProvider = resolve(),
                    httpClient = builder.createClient {
                        expectSuccess = true
                        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                            json(defaultJson)
                        }
                    }
                )
            }
            provide<AltinnTilgangerService> {
                object : AltinnTilgangerService {
                    override suspend fun hentAltinnTilganger(token: String) =
                        TODO("Not yet implemented")

                    override suspend fun harTilgang(orgnr: String, tjeneste: String, token: String) =
                        TODO("Not yet implemented")

                    override suspend fun harOrganisasjon(orgnr: String, token: String) =
                        TODO("Not yet implemented")
                }
            }
            provide(KontostatusService::class)
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()

            configureKontonummerRoutes()
        }
    ) {
        assertNull(resolve<KontoregisterClient>().hentKontonummer("123"))

        client.post("ditt-nav-arbeidsgiver-api/api/kontonummerStatus/v1") {
            contentType(ContentType.Application.Json)
            setBody("""{"virksomhetsnummer": "123"}""")
            bearerAuth("faketoken")
        }.let {
            assertEquals(HttpStatusCode.OK, it.status)
            JSONAssert.assertEquals(it.bodyAsText(), """{"status": "MANGLER_KONTONUMMER"}""", true)
        }
    }

    @Test
    fun `henter kontonummer fra kontoregister og returnerer kontonummer og orgnr`() = runTestApplication(
        externalServicesCfg = {
            mockKontoregister {
                if (call.parameters["orgnummer"] == "42") {
                    call.respondText(
                        //language=JSON
                        """
                        {
                            "mottaker": "42",
                            "kontonr": "12345678901"
                        }
                        """,
                        ContentType.Application.Json
                    )
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
            mockAltinnTilganger(AltinnTilgangerMock.medTilganger(orgnr = "42", tjeneste = kontonummerTilgangTjenesetekode))
        },
        dependenciesCfg = { builder ->
            provide<TokenXTokenIntrospector> {
                MockTokenIntrospector {
                    if (it == "faketoken") mockIntrospectionResponse.withPid("42") else null
                }
            }
            provide<AzureAdTokenProvider> { successAzureAdTokenProvider }
            provide<TokenXTokenExchanger> { successTokenXTokenExchanger }
            provide<KontoregisterClient> {
                KontoregisterClientImpl(
                    tokenProvider = resolve(),
                    httpClient = builder.createClient {
                        expectSuccess = true
                        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                            json(defaultJson)
                        }
                    }
                )
            }
            provide<AltinnTilgangerService>(AltinnTilgangerServiceImpl::class)
            provide(KontostatusService::class)
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()

            configureKontonummerRoutes()
        }
    ) {
        resolve<KontoregisterClient>().hentKontonummer("42").let {
            assertEquals("42", it?.mottaker)
            assertEquals("12345678901", it?.kontonr)
        }

        client.post("ditt-nav-arbeidsgiver-api/api/kontonummer/v1")
        {
            setBody(
                """
                {
                    "orgnrForOppslag": "42",
                    "orgnrForTilgangstyring": "42"
                }
                """.trimIndent()
            )
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth("faketoken")
        }.let {
            assertEquals(HttpStatusCode.OK, it.status)
            JSONAssert.assertEquals(it.bodyAsText(), """{"status": "OK", "orgnr": "42", "kontonummer": "12345678901"}""", true)
        }
    }

    @Test
    fun `bruker har ikke tilgang til å se kontonummer returnerer 404`() = runTestApplication(
        externalServicesCfg = {
            mockKontoregister {
                if (call.parameters["orgnummer"] == "42") {
                    call.respondText(
                        //language=JSON
                        """
                        {
                            "mottaker": "42",
                            "kontonr": "12345678901"
                        }
                        """,
                        ContentType.Application.Json
                    )
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
            mockAltinnTilganger(AltinnTilgangerMock.empty)
        },
        dependenciesCfg = { builder ->
            provide<TokenXTokenIntrospector> {
                MockTokenIntrospector {
                    if (it == "faketoken") mockIntrospectionResponse.withPid("42") else null
                }
            }
            provide<AzureAdTokenProvider> { successAzureAdTokenProvider }
            provide<TokenXTokenExchanger> { successTokenXTokenExchanger }
            provide<KontoregisterClient> {
                KontoregisterClientImpl(
                    tokenProvider = resolve(),
                    httpClient = builder.createClient {
                        expectSuccess = true
                        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                            json(defaultJson)
                        }
                    }
                )
            }
            provide<AltinnTilgangerService>(AltinnTilgangerServiceImpl::class)
            provide(KontostatusService::class)
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()

            configureKontonummerRoutes()
        }
    ) {

        resolve<KontoregisterClient>().hentKontonummer("42").let {
            assertEquals("42", it?.mottaker)
            assertEquals("12345678901", it?.kontonr)
        }

        client.post("ditt-nav-arbeidsgiver-api/api/kontonummer/v1")
        {
            setBody(
                """
                {
                    "orgnrForOppslag": "42",
                    "orgnrForTilgangstyring": "42"
                }
                """.trimIndent()
            )
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth("faketoken")
        }.let {
            assertEquals(HttpStatusCode.NotFound, it.status)
        }
    }

    @Test
    fun `kontnummer finnes ikke for virksomhet`() = runTestApplication(
        externalServicesCfg = {
            mockKontoregister {
                call.respond(HttpStatusCode.NotFound)
            }
            mockAltinnTilganger(
                AltinnTilgangerMock.medTilganger(orgnr = "42", tjeneste = kontonummerTilgangTjenesetekode)
            )
        },
        dependenciesCfg = { builder ->
            provide<TokenXTokenIntrospector> {
                MockTokenIntrospector {
                    if (it == "faketoken") mockIntrospectionResponse.withPid("42") else null
                }
            }
            provide<AzureAdTokenProvider> { successAzureAdTokenProvider }
            provide<TokenXTokenExchanger> { successTokenXTokenExchanger }
            provide<KontoregisterClient> {
                KontoregisterClientImpl(
                    tokenProvider = resolve(),
                    httpClient = builder.createClient {
                        expectSuccess = true
                        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                            json(defaultJson)
                        }
                    }
                )
            }
            provide<AltinnTilgangerService>(AltinnTilgangerServiceImpl::class)
            provide(KontostatusService::class)
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()

            configureKontonummerRoutes()
        }
    ) {

        assertNull(resolve<KontoregisterClient>().hentKontonummer("42"))

        client.post("ditt-nav-arbeidsgiver-api/api/kontonummer/v1")
        {
            setBody(
                """
                {
                    "orgnrForOppslag": "42",
                    "orgnrForTilgangstyring": "42"
                }
                """.trimIndent()
            )
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth("faketoken")
        }.let {
            assertEquals(HttpStatusCode.OK, it.status)
            JSONAssert.assertEquals(
                """{"status": "MANGLER_KONTONUMMER", "orgnr":  null, "kontonummer": null}""",
                it.bodyAsText(),
                true
            )
        }
    }
}

private fun ExternalServicesBuilder.mockKontoregister(
    handler: suspend RoutingContext.() -> Unit
) {
    hosts(KontoregisterClient.ingress) {
        install(ContentNegotiation) {
            json()
        }
        routing {
            get(KontoregisterClient.apiPath + "/{orgnummer}") {
                handler()
            }
        }
    }
}