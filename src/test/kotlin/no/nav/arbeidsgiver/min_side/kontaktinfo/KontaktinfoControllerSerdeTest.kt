package no.nav.arbeidsgiver.min_side.kontaktinfo

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.plugins.di.*
import no.nav.arbeidsgiver.min_side.FakeApi
import no.nav.arbeidsgiver.min_side.FakeApplication
import no.nav.arbeidsgiver.min_side.fakeToken
import no.nav.arbeidsgiver.min_side.maskinporten.MaskinportenTokenService
import no.nav.arbeidsgiver.min_side.maskinporten.MaskinportenTokenServiceStub
import no.nav.arbeidsgiver.min_side.services.ereg.EregClient
import no.nav.arbeidsgiver.min_side.services.kontaktinfo.KontaktInfoService
import no.nav.arbeidsgiver.min_side.services.kontaktinfo.KontaktinfoClient
import no.nav.arbeidsgiver.min_side.tilgangsstyring.AltinnRollerClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito
import org.skyscreamer.jsonassert.JSONAssert.assertEquals


class KontaktInfoServiceSerdeTest {
    companion object {
        @RegisterExtension
        val app = FakeApplication(
            addDatabase = true,
        ) {
            dependencies {
                provide<KontaktinfoClient> { Mockito.mock<KontaktinfoClient>() }
                provide<AltinnRollerClient> { Mockito.mock<AltinnRollerClient>() }
                provide<EregClient> { Mockito.mock<EregClient>() }
                provide<KontaktInfoService>(KontaktInfoService::class)
                provide<MaskinportenTokenService>(MaskinportenTokenServiceStub::class)
            }
        }

        @RegisterExtension
        val fakeApi = FakeApi()
    }

    @Test
    fun protocolFormat() = app.runTest {
        client.kontaktinfo(
            content = """{ "virksomhetsnummer": "123456789" }"""
        ).let {
            assert(HttpStatusCode.OK == it.status)
            assertEquals(
                """
                    {
                        "hovedenhet": null,
                        "underenhet": null
                    }
                """.trimIndent(), it.bodyAsText(), true
            )
        }
    }

    @Test
    fun virksomhetsnummerAsNumberFails() = app.runTest {
        /* spring's objectmapper konverterer numbers til strings. */
        client.kontaktinfo(
            content = """{ "virksomhetsnummer": 123456789 }"""
        ).let {
            assert(HttpStatusCode.OK == it.status)
        }
    }
//
//
    @Test
    fun wrongJsonInRequest() = app.runTest {
        client.kontaktinfo(
            content = """{  }"""
        ).let {
            assert(HttpStatusCode.BadRequest == it.status)
        }
    }


    @Test
    fun superflousJsonFields() = app.runTest {
        /* spring's objectmapper godtar ekstra felter. */
        client.kontaktinfo(
            content = """{ "virksomhetsnummer": "123412341", "garbage": 2 }"""
        ).let {
            assert(HttpStatusCode.OK == it.status)
        }
    }

    @Test
    fun disallowAcceptXML() = app.runTest {
        client.kontaktinfo(
            content = """{ "virksomhetsnummer": "123412341" }""",
            accept = ContentType.Application.Xml
        ).let {
            assert(it.status.value in (400 until 500))
        }
    }

    private suspend fun HttpClient.kontaktinfo(
        contentType: ContentType = ContentType.Application.Json,
        content: String,
        token: String = fakeToken("42"),
        accept: ContentType = ContentType.Application.Json,
    ): HttpResponse =
        post("/api/kontaktinfo/v1") {
            contentType(contentType)
            setBody(content)
            accept(accept)
            bearerAuth(token)
        }


}
