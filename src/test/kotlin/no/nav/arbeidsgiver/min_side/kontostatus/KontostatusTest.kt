package no.nav.arbeidsgiver.min_side.kontostatus

import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.server.plugins.di.*
import io.ktor.server.response.respond
import no.nav.arbeidsgiver.min_side.FakeApi
import no.nav.arbeidsgiver.min_side.FakeApplication
import no.nav.arbeidsgiver.min_side.azuread.AzureService
import no.nav.arbeidsgiver.min_side.fakeToken
import no.nav.arbeidsgiver.min_side.kotlinAny
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import no.nav.arbeidsgiver.min_side.services.kontostatus.KontoregisterClient
import no.nav.arbeidsgiver.min_side.services.kontostatus.KontostatusService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.skyscreamer.jsonassert.JSONAssert.assertEquals

class KontostatusTest {
    companion object {
        @RegisterExtension
        val app = FakeApplication(
            addDatabase = true,
        ) {
            dependencies {
                provide<AzureService> {
                    val service = Mockito.mock<AzureService>()
                    `when`(service.getAccessToken(kotlinAny())).thenReturn("token")
                    service
                }
                provide<AltinnService> { Mockito.mock<AltinnService>() }
                provide<KontoregisterClient>(KontoregisterClient::class)
                provide<KontostatusService>(KontostatusService::class)
            }
        }

        @RegisterExtension
        val fakeApi = FakeApi()
    }

    @Test
    fun `henter kontonummer fra kontoregister`() = app.runTest {
        val virksomhetsnummer = "42"
        fakeApi.registerStub(
            HttpMethod.Get,
            "/kontoregister/api/v1/hent-kontonummer-for-organisasjon/$virksomhetsnummer"
        ) {
            call.response.headers.append(HttpHeaders.ContentType, "application/json")
            call.respond(
                """
                {
                    "mottaker": "42",
                    "kontonr": "12345678901"
                }
                """
            )
        }


        app.getDependency<KontoregisterClient>().hentKontonummer(virksomhetsnummer).let {
            Assertions.assertEquals("42", it?.mottaker)
            Assertions.assertEquals("12345678901", it?.kontonr)
        }

        client.post("/api/kontonummerStatus/v1")
        {
            setBody("""{"virksomhetsnummer": "$virksomhetsnummer"}""")
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(fakeToken("42"))
        }.let {
            assert(it.status == HttpStatusCode.OK)
            assertEquals(it.bodyAsText(), """{"status": "OK"}""", true)
        }
    }

    @Test
    fun `finner ikke kontonummer for virksomhet`() = app.runTest {
        val virksomhetsnummer = "123"
        fakeApi.registerStub(
            HttpMethod.Get,
            "/kontoregister/api/v1/hent-kontonummer-for-organisasjon/$virksomhetsnummer"
        ) {
            call.respond(HttpStatusCode.NotFound)
        }

        app.getDependency<KontoregisterClient>().hentKontonummer(virksomhetsnummer).let { // 404 kaster exception
            Assertions.assertNull(it)
        }

        client.post("/api/kontonummerStatus/v1")
        {
            setBody("""{"virksomhetsnummer": "$virksomhetsnummer"}""")
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(fakeToken("42"))
        }.let {
            assert(it.status == HttpStatusCode.OK)
            assertEquals(it.bodyAsText(), """{"status": "MANGLER_KONTONUMMER"}""", true)
        }
    }

    @Test
    fun `henter kontonummer fra kontoregister og returnerer kontonummer og orgnr`() = app.runTest {
        val virksomhetsnummer = "42"

        fakeApi.registerStub(
            HttpMethod.Get,
            "/kontoregister/api/v1/hent-kontonummer-for-organisasjon/$virksomhetsnummer"
        ) {
            call.response.headers.append(HttpHeaders.ContentType, "application/json")
            call.respond(
                """
                {
                    "mottaker": "42",
                    "kontonr": "12345678901"
                }
                """
            )
        }

        app.getDependency<KontoregisterClient>().hentKontonummer(virksomhetsnummer).let {
            Assertions.assertEquals("42", it?.mottaker)
            Assertions.assertEquals("12345678901", it?.kontonr)
        }
        val altinnService = app.getDependency<AltinnService>()
        `when`(altinnService.harTilgang(kotlinAny(), kotlinAny(), kotlinAny())).thenReturn(true)


        client.post("/api/kontonummer/v1")
        {
            setBody(
                """
                {
                    "orgnrForOppslag": "$virksomhetsnummer",
                    "orgnrForTilgangstyring": "$virksomhetsnummer"
                }
                """.trimIndent()
            )
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(fakeToken("42"))
        }.let {
            assert(it.status == HttpStatusCode.OK)
            assertEquals(it.bodyAsText(), """{"status": "OK", "orgnr": "42", "kontonummer": "12345678901"}""", true)
        }
    }

    @Test
    fun `bruker har ikke tilgang til å se kontonummer returnerer 404`() = app.runTest {
        val virksomhetsnummer = "42"

        fakeApi.registerStub(
            HttpMethod.Get,
            "/kontoregister/api/v1/hent-kontonummer-for-organisasjon/$virksomhetsnummer"
        ) {
            call.response.headers.append(HttpHeaders.ContentType, "application/json")
            call.respond(
                """
                {
                    "mottaker": "42",
                    "kontonr": "12345678901"
                }
                """
            )
        }

        app.getDependency<KontoregisterClient>().hentKontonummer(virksomhetsnummer).let {
            Assertions.assertEquals("42", it?.mottaker)
            Assertions.assertEquals("12345678901", it?.kontonr)
        }
        val altinnService = app.getDependency<AltinnService>()
        `when`(altinnService.harTilgang(kotlinAny(), kotlinAny(), kotlinAny())).thenReturn(false)

        client.post("/api/kontonummer/v1")
        {
            setBody(
                """
                {
                    "orgnrForOppslag": "$virksomhetsnummer",
                    "orgnrForTilgangstyring": "$virksomhetsnummer"
                }
                """.trimIndent()
            )
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(fakeToken("42"))
        }.let {
            assert(it.status == HttpStatusCode.NotFound)
        }
    }

    @Test
    fun `kontnummer finnes ikke for virksomhet`() = app.runTest {
        val virksomhetsnummer = "123"

        fakeApi.registerStub(
            HttpMethod.Get,
            "/kontoregister/api/v1/hent-kontonummer-for-organisasjon/$virksomhetsnummer"
        ) {
            call.respond(HttpStatusCode.NotFound)
        }

        app.getDependency<KontoregisterClient>().hentKontonummer(virksomhetsnummer).let {
            Assertions.assertNull(it)
        }

        val altinnService = app.getDependency<AltinnService>()
        `when`(altinnService.harTilgang(kotlinAny(), kotlinAny(), kotlinAny())).thenReturn(true)

        client.post("/api/kontonummer/v1")
        {
            setBody(
                """
                {
                    "orgnrForOppslag": "$virksomhetsnummer",
                    "orgnrForTilgangstyring": "$virksomhetsnummer"
                }
                """.trimIndent()
            )
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(fakeToken("42"))
        }.let {
            assert(it.status == HttpStatusCode.OK)
            assertEquals(
                """{"status": "MANGLER_KONTONUMMER", "orgnr":  null, "kontonummer": null}""",
                it.bodyAsText(),
                true
            )
        }
    }
}