package no.nav.arbeidsgiver.min_side.controller

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.plugins.di.dependencies
import no.nav.arbeidsgiver.min_side.FakeApi
import no.nav.arbeidsgiver.min_side.FakeApplication
import no.nav.arbeidsgiver.min_side.fakeToken
import no.nav.arbeidsgiver.min_side.services.lagredefilter.LagredeFilterService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.skyscreamer.jsonassert.JSONAssert.assertEquals


class LagredeFilterIntegrationTest {
    companion object {
        @RegisterExtension
        val app = FakeApplication(addDatabase = true) {
            dependencies {
                provide<LagredeFilterService>(LagredeFilterService::class)
            }
        }

        @RegisterExtension
        val fakeApi = FakeApi() // Brukes som mock token introspection server
    }


    @Test
    fun `returnerer tomt array når bruker har ingen lagrede filter`() = app.runTest {
        client.get("/api/lagredeFilter/") {
            bearerAuth(fakeToken("42"))
        }.let {
            assert(it.status == HttpStatusCode.OK)
            assertEquals("[]", it.bodyAsText(), true)
        }
    }


    @Test
    fun `lagrer, returnerer, oppdaterer og sletter filter`() = app.runTest {
        val filterJson = """
                        {
                            "filterId": "filter1",
                            "navn": "uløste oppgaver",
                            "side": 1,
                            "tekstsoek": "",
                            "virksomheter": [],
                            "sortering": "NYESTE",
                            "sakstyper": [],
                            "oppgaveFilter": ["TILSTAND_NY"]
                        }
                    """.trimIndent()

        val filterJson2 = """
                        {
                            "filterId": "filter2",
                            "navn": " oppgaver med påminnelse for virksomhet 123456789",
                            "side": 1,
                            "tekstsoek": "",
                            "virksomheter": ["123456789"],
                            "sortering": "NYESTE",
                            "sakstyper": [],
                            "oppgaveFilter": ["TILSTAND_NY_MED_PÅMINNELSE"]
                        }
                    """.trimIndent()

        client.put("/api/lagredeFilter") {
            bearerAuth(fakeToken("42"))
            contentType(ContentType.Application.Json)
            setBody(filterJson)
        }

        client.put("/api/lagredeFilter") {
            bearerAuth(fakeToken("42"))
            contentType(ContentType.Application.Json)
            setBody(filterJson2)
        }


        client.get("/api/lagredeFilter") {
            bearerAuth(fakeToken("42"))
        }.bodyAsText().also { assertEquals("[${filterJson},${filterJson2}]", it, true) }

        val oppdatertFilterJson2 = """
                        {
                            "filterId": "filter2",
                            "navn": " oppgaver med påminnelse",
                            "side": 1,
                            "tekstsoek": "",
                            "virksomheter": [],
                            "sortering": "ELDSTE",
                            "sakstyper": [],
                            "oppgaveFilter": ["TILSTAND_NY_MED_PÅMINNELSE"]
                        }
                    """.trimIndent()

        client.put("/api/lagredeFilter") {
            bearerAuth(fakeToken("42"))
            contentType(ContentType.Application.Json)
            setBody(oppdatertFilterJson2)
        }

        client.get("/api/lagredeFilter") {
            bearerAuth(fakeToken("42"))
        }.bodyAsText().also { assertEquals("[${filterJson},${oppdatertFilterJson2}]", it, true) }

        client.delete("/api/lagredeFilter/filter1") {
            bearerAuth(fakeToken("42"))
        }

        client.get("/api/lagredeFilter") {
            bearerAuth(fakeToken("42"))
        }.bodyAsText().also { assertEquals("[${oppdatertFilterJson2}]", it, true) }
    }
}