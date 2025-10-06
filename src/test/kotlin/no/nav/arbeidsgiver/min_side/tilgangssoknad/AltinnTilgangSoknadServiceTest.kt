package no.nav.arbeidsgiver.min_side.tilgangssoknad

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.plugins.di.*
import kotlinx.coroutines.runBlocking
import no.nav.arbeidsgiver.min_side.FakeApi
import no.nav.arbeidsgiver.min_side.FakeApplication
import no.nav.arbeidsgiver.min_side.fakeToken
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.skyscreamer.jsonassert.JSONAssert
import java.nio.charset.Charset


class AltinnTilgangSoknadServiceTest {
    companion object {
        @RegisterExtension
        val app = FakeApplication(
            addDatabase = true,
        ) {
            dependencies {
                provide<AltinnTilgangssøknadClient> { Mockito.mock<AltinnTilgangssøknadClient>() }
                provide<AltinnTilgangSoknadService>(AltinnTilgangSoknadService::class)
                provide<AltinnService> { Mockito.mock<AltinnService>() }
            }
        }

        @RegisterExtension
        val fakeApi = FakeApi()
    }

    @Test
    fun mineSøknaderOmTilgang() = app.runTest {
        val altinnTilgangssøknad = AltinnTilgangssøknad(
            orgnr = "314",
            serviceCode = "13337",
            serviceEdition = 3,
            status = "Created",
            createdDateTime = "now",
            lastChangedDateTime = "whenever",
            submitUrl = "https://yolo.com",
        )
        val altinnTilgangssøknadClient = app.getDependency<AltinnTilgangssøknadClient>()
        `when`(altinnTilgangssøknadClient.hentSøknader("42")).thenReturn(listOf(altinnTilgangssøknad))

        val jsonResponse = client.get("/api/altinn-tilgangssoknad") {
            bearerAuth(fakeToken("42"))
            accept(ContentType.Application.Json)
        }.let {
            assert(it.status == HttpStatusCode.OK)
            it.bodyAsText()
        }

        JSONAssert.assertEquals(
            """
            [
              {
                "orgnr": "314",
                "serviceCode": "13337",
                "serviceEdition": 3,
                "status": "Created",
                "createdDateTime": "now",
                "lastChangedDateTime": "whenever",
                "submitUrl": "https://yolo.com"
              }
            ]
            """,
            jsonResponse,
            true
        )
    }

    @Test
    fun sendSøknadOmTilgang() = app.runTest {
        val skjema = AltinnTilgangssøknadsskjema(
            orgnr = "314",
            redirectUrl = "https://yolo.it",
            serviceCode = AltinnTilgangSoknadService.tjenester.first().first,
            serviceEdition = AltinnTilgangSoknadService.tjenester.first().second,
        )
        val søknad = AltinnTilgangssøknad(
            orgnr = "314",
            serviceCode = "13337",
            serviceEdition = 3,
            status = "Created",
            createdDateTime = "now",
            lastChangedDateTime = "whenever",
            submitUrl = "https://yolo.com",
        )


        val altinnService = app.getDependency<AltinnService>()
        val altinnTilgangssøknadClient = app.getDependency<AltinnTilgangssøknadClient>()
        val token = fakeToken("42")

        `when`(altinnService.harOrganisasjon(skjema.orgnr, token)).thenReturn(true)
        `when`(altinnTilgangssøknadClient.sendSøknad("42", skjema)).thenReturn(søknad)

        val jsonResponse = client
            .post("/api/altinn-tilgangssoknad") {
                bearerAuth(token)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(
                    """
                        {
                            "orgnr": "${skjema.orgnr}",
                            "redirectUrl": "${skjema.redirectUrl}",
                            "serviceCode": "${skjema.serviceCode}",
                            "serviceEdition": ${skjema.serviceEdition}
                        }
                    """
                )
            }.let {
                assert(it.status == HttpStatusCode.OK)
                it.bodyAsText()
            }

        JSONAssert.assertEquals(
            """
              {
                "orgnr": "${søknad.orgnr}",
                "serviceCode": "${søknad.serviceCode}",
                "serviceEdition": ${søknad.serviceEdition},
                "status": "${søknad.status}",
                "createdDateTime": "${søknad.createdDateTime}",
                "lastChangedDateTime": "${søknad.lastChangedDateTime}",
                "submitUrl": "${søknad.submitUrl}"
              }
            """,
            jsonResponse,
            true
        )
    }

    @Test
    fun sendSøknadOmTilgangSomAlleredeErSøktPåGirBadRequest() = app.runTest {
        val skjema = AltinnTilgangssøknadsskjema(
            orgnr = "314",
            redirectUrl = "https://yolo.it",
            serviceCode = AltinnTilgangSoknadService.tjenester.first().first,
            serviceEdition = AltinnTilgangSoknadService.tjenester.first().second,
        )

        val altinnService = app.getDependency<AltinnService>()
        val altinnTilgangssøknadClient = app.getDependency<AltinnTilgangssøknadClient>()
        val token = fakeToken("42")

        `when`(altinnService.harOrganisasjon("314", token)).thenReturn(true)
        `when`(altinnTilgangssøknadClient.sendSøknad("42", skjema)).then {
            val response = Mockito.mock<HttpResponse>()
            `when`(response.status).thenReturn(HttpStatusCode.BadRequest)
            runBlocking {
                `when`(response.bodyAsText()).thenReturn(
                    """[{"ErrorCode":"40318","ErrorMessage":"This request for access has already been registered"}]"""
                )
            }

            throw ClientRequestException(
                response,
                """[{"ErrorCode":"40318","ErrorMessage":"This request for access has already been registered"}]""",
            )
        }

        client
            .post("/api/altinn-tilgangssoknad")
            {
                bearerAuth(token)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(
                    """
                        {
                            "orgnr": "${skjema.orgnr}",
                            "redirectUrl": "${skjema.redirectUrl}",
                            "serviceCode": "${skjema.serviceCode}",
                            "serviceEdition": ${skjema.serviceEdition}
                        }
                    """
                )
            }.let {
                assert(it.status == HttpStatusCode.BadRequest)
            }

    }
}