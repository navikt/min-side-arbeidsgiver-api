package no.nav.arbeidsgiver.min_side

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.arbeidsgiver.min_side.infrastruktur.*
import no.nav.arbeidsgiver.min_side.services.lagredefilter.LagredeFilterService
import org.skyscreamer.jsonassert.JSONAssert
import kotlin.test.Test
import kotlin.test.assertEquals

class LagredeFilterApiTest {


    @Test
    fun `returnerer tomt array når bruker har ingen lagrede filter`() = runTestApplicationWithDatabase(
        dependenciesCfg = {
            provide<TokenXTokenIntrospector> {
                MockTokenIntrospector {
                    if (it == "faketoken") mockIntrospectionResponse.withPid("42") else null
                }
            }
            provide(LagredeFilterService::class)
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()
            configureLagredefilterRoutes()
        }
    ) {
        client.get("ditt-nav-arbeidsgiver-api/api/lagredeFilter/") {
            bearerAuth("faketoken")
        }.let {
            assertEquals(HttpStatusCode.OK, it.status)
            JSONAssert.assertEquals("[]", it.bodyAsText(), true)
        }
    }


    @Test
    fun `lagrer, returnerer, oppdaterer og sletter filter`() = runTestApplicationWithDatabase(
        dependenciesCfg = {
            provide<TokenXTokenIntrospector> {
                MockTokenIntrospector {
                    if (it == "faketoken") mockIntrospectionResponse.withPid("42") else null
                }
            }
            provide(LagredeFilterService::class)
        },
        applicationCfg = {
            ktorConfig()
            configureTokenXAuth()
            configureLagredefilterRoutes()
        }
    ) {
        // language=JSON
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

        // language=JSON
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

        client.put("ditt-nav-arbeidsgiver-api/api/lagredeFilter") {
            bearerAuth("faketoken")
            contentType(ContentType.Application.Json)
            setBody(filterJson)
        }

        client.put("ditt-nav-arbeidsgiver-api/api/lagredeFilter") {
            bearerAuth("faketoken")
            contentType(ContentType.Application.Json)
            setBody(filterJson2)
        }


        client.get("ditt-nav-arbeidsgiver-api/api/lagredeFilter") {
            bearerAuth("faketoken")
        }.bodyAsText().also { JSONAssert.assertEquals("[${filterJson},${filterJson2}]", it, true) }

        // language=JSON
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

        client.put("ditt-nav-arbeidsgiver-api/api/lagredeFilter") {
            bearerAuth("faketoken")
            contentType(ContentType.Application.Json)
            setBody(oppdatertFilterJson2)
        }

        client.get("ditt-nav-arbeidsgiver-api/api/lagredeFilter") {
            bearerAuth("faketoken")
        }.bodyAsText().also { JSONAssert.assertEquals("[${filterJson},${oppdatertFilterJson2}]", it, true) }

        client.delete("ditt-nav-arbeidsgiver-api/api/lagredeFilter/filter1") {
            bearerAuth("faketoken")
        }

        client.get("ditt-nav-arbeidsgiver-api/api/lagredeFilter") {
            bearerAuth("faketoken")
        }.bodyAsText().also { JSONAssert.assertEquals("[${oppdatertFilterJson2}]", it, true) }
    }
}