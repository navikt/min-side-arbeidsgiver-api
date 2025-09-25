package no.nav.arbeidsgiver.min_side.kontaktinfo

import io.ktor.http.*
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.response.*
import no.nav.arbeidsgiver.min_side.FakeApi
import no.nav.arbeidsgiver.min_side.FakeApplication
import no.nav.arbeidsgiver.min_side.maskinporten.MaskinportenTokenService
import no.nav.arbeidsgiver.min_side.maskinporten.MaskinportenTokenServiceStub
import no.nav.arbeidsgiver.min_side.services.kontaktinfo.KontaktinfoClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.web.client.HttpClientErrorException

@RestClientTest(
    KontaktinfoClient::class,
    MaskinportenTokenServiceStub::class,
)
class KontaktinfoClientTest {
    companion object {
        @RegisterExtension
        val app = FakeApplication(
            addDatabase = true,
        ) {
            dependencies {
                provide<KontaktinfoClient>(KontaktinfoClient::class)
                provide<MaskinportenTokenService>(MaskinportenTokenServiceStub::class)
            }
        }

        @RegisterExtension
        val fakeApi = FakeApi()
    }

    /* NB. Har sjekket hvordan responsen påvirkes av sletting av kontaktinfo, og
     * de slettede blir bare fjernet fra listen; ingen markering eller noe sånt noe.
     */

    @Test
    fun telefonnummerErRegistrert() = app.runTest {
        mockKontaktinfoResponse(orgnr = "1", kunTelefonnumerResponse)

        val kontaktinfo = app.getDependency<KontaktinfoClient>().hentKontaktinfo("1")
        assertEquals(
            KontaktinfoClient.Kontaktinfo(
                telefonnumre = setOf("+4711223344"),
                eposter = setOf(),
            ),
            kontaktinfo
        )
    }

    @Test
    fun epostErRegistrert() = app.runTest {
        mockKontaktinfoResponse(orgnr = "2", kunEpostResponse)
        val kontaktinfo = app.getDependency<KontaktinfoClient>().hentKontaktinfo("2")
        assertEquals(
            KontaktinfoClient.Kontaktinfo(
                telefonnumre = setOf(),
                eposter = setOf("test@test.no"),
            ),
            kontaktinfo
        )
    }

    @Test
    fun bådeTlfOgEpostRegistrert() = app.runTest {
        mockKontaktinfoResponse(orgnr = "1234", bådeTlfOgEpostResponse)
        val kontaktinfo = app.getDependency<KontaktinfoClient>().hentKontaktinfo("1234")
        assertEquals(
            KontaktinfoClient.Kontaktinfo(
                telefonnumre = setOf("+4700112233"),
                eposter = setOf("foo@example.com"),
            ),
            kontaktinfo
        )

    }

    @Test
    fun ingenKontaktinfoRegistrert() = app.runTest {
        mockKontaktinfoResponse(orgnr = "3", ingenKontaktinfoResponse)

        val kontaktinfo = app.getDependency<KontaktinfoClient>().hentKontaktinfo("3")
        assertEquals(
            KontaktinfoClient.Kontaktinfo(
                telefonnumre = setOf(),
                eposter = setOf(),
            ),
            kontaktinfo
        )
    }

    @Test
    fun flereEposterRegistrert() = app.runTest {
        mockKontaktinfoResponse(orgnr = "3", flereEposterRequest)

        val kontaktinfo = app.getDependency<KontaktinfoClient>().hentKontaktinfo("3")
        assertEquals(
            KontaktinfoClient.Kontaktinfo(
                telefonnumre = setOf("+4700112233"),
                eposter = setOf("foo@example.com", "foo3@example.com", "foo2@example.com"),
            ),
            kontaktinfo
        )
    }

    @Test
    fun organisasjonFinnesIkke(): Unit = app.runTest {
        fakeApi.stubs.put(
            Pair(
                io.ktor.http.HttpMethod.Get,
                "/api/serviceowner/organizations/1/officialcontacts"
            ),
            { call.respond(HttpStatusCode.BadRequest) }
        )

//        altinnServer.expect {
//            assertEquals("/api/serviceowner/organizations/1/officialcontacts", it.uri.path)
//            assertNotNull("query-parameters må være gitt", it.uri.query)
//            assertTrue(it.uri.query.contains("ForceEIAuthentication"))
//        }.andRespond(withBadRequest())

        /* Hvis orgnr ikke finnes får man responsen:
         * HTTP/1.1 400 Invalid organization number: 0000000 */
        assertThrows<HttpClientErrorException.BadRequest> {
            app.getDependency<KontaktinfoClient>().hentKontaktinfo("1")
        }
    }

    private fun mockKontaktinfoResponse(orgnr: String, response: String) =
        fakeApi.stubs.put(
            Pair(
                HttpMethod.Get,
                "/api/serviceowner/organizations/${orgnr}/officialcontacts"
            ),
            {
                assertNotNull(call.request.queryParameters)
                call.respond(response)
            }
        )

//            .expect {
//                assertEquals(HttpMethod.GET, it.method)
//                assertEquals("/api/serviceowner/organizations/${orgnr}/officialcontacts", it.uri.path)
//                assertNotNull("query-parameters må være gitt", it.uri.query)
//                assertTrue(
//                    it.uri.query.contains("ForceEIAuthentication"),
//                    "altinn forventer spesiell header for autentiserting"
//                )
//                assertTrue(it.headers.getFirst("authorization")!!.startsWith("Bearer"), "bearer token må være satt")
//                assertTrue(it.headers.getFirst("apikey")!!.isNotBlank(), "apikey må være satt")
//            }.andRespond(
//                withSuccess(response, APPLICATION_JSON)
//            )
}

/* Hentet ved kall mot tt02.altinn.no */
private val kunTelefonnumerResponse = """
    [
      {
        "MobileNumber": "+4711223344",
        "MobileNumberChanged": "2021-10-05T13:04:19.367",
        "EMailAddress": "",
        "EMailAddressChanged": "0001-01-01T00:00:00"
      }
    ]
"""

/* Hentet ved kall mot tt02.altinn.no */
private val kunEpostResponse = """
    [
  {
    "MobileNumber": "",
    "MobileNumberChanged": "0001-01-01T00:00:00",
    "EMailAddress": "test@test.no",
    "EMailAddressChanged": "2021-03-15T10:42:08.287"
  }
]
"""

/*  Hentet fra tt02.altinn.no */
private val bådeTlfOgEpostResponse = """
[
    {
        "MobileNumber": "+4700112233",
        "MobileNumberChanged": "2021-10-05T13:04:19.367",
        "EMailAddress": "",
        "EMailAddressChanged": "0001-01-01T00:00:00"
    },
    {
        "MobileNumber": "",
        "MobileNumberChanged": "0001-01-01T00:00:00",
        "EMailAddress": "foo@example.com",
        "EMailAddressChanged": "2023-10-12T15:17:15.497"
    }
]
"""

/* Hentet fra kall mot tt02.altinn.no */
private val ingenKontaktinfoResponse = """
    []
"""


/* Legger på 3 eposter på en gang fra altinns GUI */
private val flereEposterRequest = """
    [
      {
        "MobileNumber": "+4700112233",
        "MobileNumberChanged": "2021-10-05T13:04:19.367",
        "EMailAddress": "",
        "EMailAddressChanged": "0001-01-01T00:00:00"
      },
      {
        "MobileNumber": "",
        "MobileNumberChanged": "0001-01-01T00:00:00",
        "EMailAddress": "foo@example.com",
        "EMailAddressChanged": "2023-10-12T15:17:15.497"
      },
      {
        "MobileNumber": "",
        "MobileNumberChanged": "0001-01-01T00:00:00",
        "EMailAddress": "foo3@example.com",
        "EMailAddressChanged": "2023-10-12T15:38:44.297"
      },
      {
        "MobileNumber": "",
        "MobileNumberChanged": "0001-01-01T00:00:00",
        "EMailAddress": "foo2@example.com",
        "EMailAddressChanged": "2023-10-12T15:38:44.437"
      }
    ]
"""