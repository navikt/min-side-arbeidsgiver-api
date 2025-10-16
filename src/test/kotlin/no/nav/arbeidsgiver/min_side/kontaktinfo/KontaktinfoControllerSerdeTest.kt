package no.nav.arbeidsgiver.min_side.kontaktinfo

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.plugins.di.*
import no.nav.arbeidsgiver.min_side.FakeApi
import no.nav.arbeidsgiver.min_side.FakeApplication
import no.nav.arbeidsgiver.min_side.fakeToken
import no.nav.arbeidsgiver.min_side.kotlinAny
import no.nav.arbeidsgiver.min_side.maskinporten.MaskinportenTokenService
import no.nav.arbeidsgiver.min_side.maskinporten.MaskinportenTokenServiceStub
import no.nav.arbeidsgiver.min_side.services.ereg.EregClient
import no.nav.arbeidsgiver.min_side.services.ereg.EregEnhetsRelasjon
import no.nav.arbeidsgiver.min_side.services.ereg.EregNavn
import no.nav.arbeidsgiver.min_side.services.ereg.EregOrganisasjon
import no.nav.arbeidsgiver.min_side.services.ereg.EregOrganisasjonDetaljer
import no.nav.arbeidsgiver.min_side.services.kontaktinfo.KontaktInfoService
import no.nav.arbeidsgiver.min_side.services.kontaktinfo.KontaktinfoClient
import no.nav.arbeidsgiver.min_side.tilgangsstyring.AltinnRollerClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.skyscreamer.jsonassert.JSONAssert.assertEquals


class KontaktInfoServiceSerdeTest {
    companion object {
        @RegisterExtension
        val app = FakeApplication(
            addDatabase = true,
        ) {
            dependencies {
                provide<KontaktinfoClient> {
                    val mock = Mockito.mock<KontaktinfoClient>()
                    `when`(mock.hentKontaktinfo(kotlinAny())).thenReturn(KontaktinfoClient.Kontaktinfo(emptySet(), emptySet()))
                    mock
                }
                provide<AltinnRollerClient> {
                    val mock = Mockito.mock<AltinnRollerClient>()
                    `when`(mock.harAltinnRolle(kotlinAny(), kotlinAny(), kotlinAny(), kotlinAny())).thenReturn(true)
                    mock
                }
                provide<EregClient> {
                    val mock = Mockito.mock<EregClient>()
                    `when`(mock.hentUnderenhet(kotlinAny())).thenReturn(dummyEregOrganisasjon)
                    mock
                }
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
        client.kontaktinfo(
            content = """{ "virksomhetsnummer": "123412341", "garbage": 2 }"""
        ).let {
            assert(HttpStatusCode.OK == it.status)
        }
    }

    private suspend fun HttpClient.kontaktinfo(
        contentType: ContentType = ContentType.Application.Json,
        content: String,
        token: String = fakeToken("42"),
        accept: ContentType = ContentType.Application.Json,
    ): HttpResponse =
        post("ditt-nav-arbeidsgiver-api/api/kontaktinfo/v1") {
            contentType(contentType)
            setBody(content)
            accept(accept)
            bearerAuth(token)
        }
}

val dummyEregRelasjon = EregEnhetsRelasjon("987654321", null)

val dummyEregOrganisasjon = EregOrganisasjon(
    navn = EregNavn("Underenhet AS", null),
    organisasjonsnummer = "123456789",
    type = "BEDR",
    organisasjonDetaljer = EregOrganisasjonDetaljer(null, null, null, null, null, null),
    inngaarIJuridiskEnheter = listOf(dummyEregRelasjon),
    bestaarAvOrganisasjonsledd = listOf()
)

