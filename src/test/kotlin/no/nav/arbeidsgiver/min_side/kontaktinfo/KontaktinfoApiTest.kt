package no.nav.arbeidsgiver.min_side.kontaktinfo

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.arbeidsgiver.min_side.configureKontaktinfoRoutes
import no.nav.arbeidsgiver.min_side.infrastruktur.*
import no.nav.arbeidsgiver.min_side.ktorConfig
import no.nav.arbeidsgiver.min_side.services.ereg.*
import no.nav.arbeidsgiver.min_side.services.kontaktinfo.KontaktInfoService
import no.nav.arbeidsgiver.min_side.services.kontaktinfo.KontaktinfoClient
import no.nav.arbeidsgiver.min_side.services.kontaktinfo.KontaktinfoClient.Kontaktinfo
import no.nav.arbeidsgiver.min_side.tilgangsstyring.AltinnRollerClient
import org.skyscreamer.jsonassert.JSONAssert
import kotlin.test.Test
import kotlin.test.assertEquals

class KontaktinfoApiTest {
    private val mockTokenIntrospector = MockTokenIntrospector {
        if (it == "faketoken") mockIntrospectionResponse.withPid("42") else null
    }

    private val dummyEregClient = object : EregClient {
        override suspend fun hentOrganisasjon(orgnummer: String) = dummyEregOrganisasjon
    }

    private val dummyKontaktinfoClient = object : KontaktinfoClient {
        override suspend fun hentKontaktinfo(orgnr: String) =
            Kontaktinfo(
                eposter = emptySet(),
                telefonnumre = emptySet()
            )
    }

    private val dummyAltinnRollerClient = object : AltinnRollerClient {
        override suspend fun harAltinnRolle(
            fnr: String,
            orgnr: String,
            altinnRoller: Set<String>,
            externalRoller: Set<String>
        ) = true
    }

    @Test
    fun protocolFormat() = runTestApplication(
        dependenciesCfg = {
            provide<TokenXTokenIntrospector> { mockTokenIntrospector }

            provide<EregClient> { dummyEregClient }
            provide<KontaktinfoClient> { dummyKontaktinfoClient }
            provide<AltinnRollerClient> { dummyAltinnRollerClient }

            provide(KontaktInfoService::class)
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()

            configureKontaktinfoRoutes()
        }
    ) {
        client.post(
            "ditt-nav-arbeidsgiver-api/api/kontaktinfo/v1"
        ) {
            contentType(ContentType.Application.Json)
            setBody(
                body = """{ "virksomhetsnummer": "123456789" }"""
            )
            bearerAuth("faketoken")
        }.let {
            assertEquals(HttpStatusCode.OK, it.status)
            JSONAssert.assertEquals(
                """
                    {
                      "hovedenhet" : {
                        "eposter" : [ ],
                        "telefonnumre" : [ ]
                      },
                      "underenhet" : {
                        "eposter" : [ ],
                        "telefonnumre" : [ ]
                      }
                    }
                """.trimIndent(), it.bodyAsText(), true
            )
        }
    }

    @Test
    fun virksomhetsnummerAsNumberFails() = runTestApplication(
        dependenciesCfg = {
            provide<TokenXTokenIntrospector> { mockTokenIntrospector }

            provide<EregClient> { dummyEregClient }
            provide<KontaktinfoClient> { dummyKontaktinfoClient }
            provide<AltinnRollerClient> { dummyAltinnRollerClient }

            provide(KontaktInfoService::class)
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()

            configureKontaktinfoRoutes()
        }
    ) {
        /* spring's objectmapper konverterer numbers til strings. */
        client.post(
            "ditt-nav-arbeidsgiver-api/api/kontaktinfo/v1"
        ) {
            contentType(ContentType.Application.Json)
            setBody(
                body = """{ "virksomhetsnummer": 123456789 }"""
            )
            bearerAuth("faketoken")
        }.let {
            assertEquals(HttpStatusCode.OK, it.status)
        }
    }

    @Test
    fun wrongJsonInRequest() = runTestApplication(
        dependenciesCfg = {
            provide<TokenXTokenIntrospector> { mockTokenIntrospector }

            provide<EregClient> { dummyEregClient }
            provide<KontaktinfoClient> { dummyKontaktinfoClient }
            provide<AltinnRollerClient> { dummyAltinnRollerClient }

            provide(KontaktInfoService::class)
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()

            configureKontaktinfoRoutes()
        }
    ) {
        client.post(
            "ditt-nav-arbeidsgiver-api/api/kontaktinfo/v1"
        ) {
            contentType(ContentType.Application.Json)
            setBody(
                body = """{  }"""
            )
            bearerAuth("faketoken")
        }.let {
            assertEquals(HttpStatusCode.BadRequest, it.status)
        }
    }


    @Test
    fun superflousJsonFields() = runTestApplication(
        dependenciesCfg = {
            provide<TokenXTokenIntrospector> { mockTokenIntrospector }

            provide<EregClient> { dummyEregClient }
            provide<KontaktinfoClient> { dummyKontaktinfoClient }
            provide<AltinnRollerClient> { dummyAltinnRollerClient }

            provide(KontaktInfoService::class)
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()

            configureKontaktinfoRoutes()
        }
    ) {
        client.post(
            "ditt-nav-arbeidsgiver-api/api/kontaktinfo/v1"
        ) {
            contentType(ContentType.Application.Json)
            setBody(
                body = """{ "virksomhetsnummer": "123412341", "garbage": 2 }"""
            )
            bearerAuth("faketoken")
        }.let {
            assertEquals(HttpStatusCode.OK, it.status)
        }
    }
}

private val dummyEregRelasjon = EregEnhetsRelasjon("987654321", null)

private val dummyEregOrganisasjon = EregOrganisasjon(
    navn = EregNavn("Underenhet AS", null),
    organisasjonsnummer = "123456789",
    type = "BEDR",
    organisasjonDetaljer = EregOrganisasjonDetaljer(null, null, null, null, null, null),
    inngaarIJuridiskEnheter = listOf(dummyEregRelasjon),
    bestaarAvOrganisasjonsledd = listOf()
)