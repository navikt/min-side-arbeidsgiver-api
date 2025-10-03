package no.nav.arbeidsgiver.min_side.tiltak

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.plugins.di.*
import no.nav.arbeidsgiver.min_side.FakeApi
import no.nav.arbeidsgiver.min_side.FakeApplication
import no.nav.arbeidsgiver.min_side.fakeToken
import no.nav.arbeidsgiver.min_side.provideDefaultObjectMapper
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilganger
import no.nav.arbeidsgiver.min_side.services.digisyfo.DigisyfoService
import no.nav.arbeidsgiver.min_side.services.digisyfo.RefusjonStatusRecordProcessor
import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusRepository
import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusService
import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusService.Companion.TJENESTEKODE
import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusService.Companion.TJENESTEVERSJON
import no.nav.arbeidsgiver.min_side.userinfo.UserInfoService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.skyscreamer.jsonassert.JSONAssert.assertEquals
import java.util.UUID.randomUUID

class RefusjonStatusIntegrationTest {
    companion object {
        @RegisterExtension
        val app = FakeApplication(
            addDatabase = true,
        ) {
            dependencies {
                provide<UserInfoService>(UserInfoService::class)
                provide<RefusjonStatusService>(RefusjonStatusService::class)
                provide<RefusjonStatusRepository>(RefusjonStatusRepository::class)
                provideDefaultObjectMapper()
                provide<DigisyfoService> { Mockito.mock<DigisyfoService>() }
                provide<AltinnService> { Mockito.mock<AltinnService>() }
            }
        }

        @RegisterExtension
        val fakeApi = FakeApi()
    }

    @Test
    fun `statusoversikt returnerer riktig innhold`() = app.runTest {
        val token = fakeToken("42")
        `when`(app.getDependency<DigisyfoService>().hentVirksomheterOgSykmeldte("42"))
            .thenReturn(listOf())
        `when`(
            app.getDependency<AltinnService>().hentAltinnTilganger(token)
        ).thenReturn(
            AltinnTilganger(
                isError = false,
                hierarki = listOf(
                    AltinnTilganger.AltinnTilgang(
                        orgnr = "1",
                        altinn3Tilganger = setOf(),
                        altinn2Tilganger = setOf(),
                        underenheter = listOf(
                            AltinnTilganger.AltinnTilgang(
                                orgnr = "314",
                                altinn3Tilganger = setOf(),
                                altinn2Tilganger = setOf("$TJENESTEKODE:$TJENESTEVERSJON"),
                                underenheter = listOf(),
                                navn = "Foo & Co",
                                organisasjonsform = "BEDR"
                            ),
                            AltinnTilganger.AltinnTilgang(
                                orgnr = "315",
                                altinn3Tilganger = setOf(),
                                altinn2Tilganger = setOf("$TJENESTEKODE:$TJENESTEVERSJON"),
                                underenheter = listOf(),
                                navn = "Bar ltd.",
                                organisasjonsform = "BEDR"
                            ),
                        ),
                        navn = "overenhet",
                        organisasjonsform = "AS"
                    ),
                ),
                orgNrTilTilganger = mapOf(
                    "314" to setOf("$TJENESTEKODE:$TJENESTEVERSJON"),
                    "315" to setOf("$TJENESTEKODE:$TJENESTEVERSJON"),
                ),
                tilgangTilOrgNr = mapOf(
                    "$TJENESTEKODE:$TJENESTEVERSJON" to setOf("314", "315"),
                )
            )
        )
        app.processRefusjonStatus("314", "ny")
        app.processRefusjonStatus("314", "gammel")
        app.processRefusjonStatus("314", "gammel")
        app.processRefusjonStatus("315", "ny")
        app.processRefusjonStatus("315", "ny")
        app.processRefusjonStatus("315", "gammel")

        // fjernes og erstattes av userinfo-endepunktet under
        client.get("/api/refusjon_status") {
            bearerAuth(token)
        }.also {
            assert(it.status == HttpStatusCode.OK)
            assertEquals(
                """
                    [
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
                    """, it.bodyAsText(), true
            )
        }

        client.get("/api/userInfo/v3") {
            bearerAuth(token)
        }.also {
            assert(it.status == HttpStatusCode.OK)
            assertEquals(
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

suspend fun FakeApplication.processRefusjonStatus(vnr: String, status: String) {
    val processor =
        RefusjonStatusRecordProcessor(getDependency<ObjectMapper>(), getDependency<RefusjonStatusRepository>())
    processor.processRecord(
        ConsumerRecord(
            "", 0, 0, "",
            """
                {
                    "refusjonId": "${randomUUID()}",
                    "bedriftNr": "$vnr",
                    "avtaleId": "${randomUUID()}",
                    "status": "$status"
                }
                """
        )
    )
}
