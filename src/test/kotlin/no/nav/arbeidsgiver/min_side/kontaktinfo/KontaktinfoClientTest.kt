package no.nav.arbeidsgiver.min_side.kontaktinfo

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.arbeidsgiver.min_side.infrastruktur.runTestApplication
import no.nav.arbeidsgiver.min_side.infrastruktur.successMaskinportenTokenProvider
import no.nav.arbeidsgiver.min_side.services.kontaktinfo.KontaktinfoClient.Kontaktinfo
import no.nav.arbeidsgiver.min_side.services.kontaktinfo.KontaktinfoClientImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class KontaktinfoClientTest {

    /* NB. Har sjekket hvordan responsen påvirkes av sletting av kontaktinfo, og
     * de slettede blir bare fjernet fra listen; ingen markering eller noe sånt noe.
     */

    @Test
    fun telefonnummerErRegistrert() = runTestApplication(
        externalServicesCfg = {
            hosts(KontaktinfoClientImpl.ingress) {
                routing {
                    get("/api/serviceowner/organizations/1/officialcontacts") {
                        call.respondText(kunTelefonnumerResponse, ContentType.Application.Json)
                    }
                }
            }
        }
    ) {
        val kontaktinfo = KontaktinfoClientImpl(client, successMaskinportenTokenProvider).hentKontaktinfo("1")
        assertEquals(
            Kontaktinfo(
                telefonnumre = setOf("+4711223344"),
                eposter = setOf(),
            ),
            kontaktinfo
        )
    }

    @Test
    fun epostErRegistrert() = runTestApplication(
        externalServicesCfg = {
            hosts(KontaktinfoClientImpl.ingress) {
                routing {
                    get("/api/serviceowner/organizations/2/officialcontacts") {
                        call.respondText(kunEpostResponse, ContentType.Application.Json)
                    }
                }
            }
        }
    ) {
        val kontaktinfo = KontaktinfoClientImpl(client, successMaskinportenTokenProvider).hentKontaktinfo("2")
        assertEquals(
            Kontaktinfo(
                telefonnumre = setOf(),
                eposter = setOf("test@test.no"),
            ),
            kontaktinfo
        )
    }

    @Test
    fun bådeTlfOgEpostRegistrert() = runTestApplication(
        externalServicesCfg = {
            hosts(KontaktinfoClientImpl.ingress) {
                routing {
                    get("/api/serviceowner/organizations/1234/officialcontacts") {
                        call.respondText(bådeTlfOgEpostResponse, ContentType.Application.Json)
                    }
                }
            }
        }
    ) {
        val kontaktinfo = KontaktinfoClientImpl(client, successMaskinportenTokenProvider).hentKontaktinfo("1234")
        assertEquals(
            Kontaktinfo(
                telefonnumre = setOf("+4700112233"),
                eposter = setOf("foo@example.com"),
            ),
            kontaktinfo
        )

    }

    @Test
    fun ingenKontaktinfoRegistrert() = runTestApplication(
        externalServicesCfg = {
            hosts(KontaktinfoClientImpl.ingress) {
                routing {
                    get("/api/serviceowner/organizations/3/officialcontacts") {
                        call.respondText(ingenKontaktinfoResponse, ContentType.Application.Json)
                    }
                }
            }
        }
    ) {
        val kontaktinfo = KontaktinfoClientImpl(client, successMaskinportenTokenProvider).hentKontaktinfo("3")
        assertEquals(
            Kontaktinfo(
                telefonnumre = setOf(),
                eposter = setOf(),
            ),
            kontaktinfo
        )
    }

    @Test
    fun flereEposterRegistrert() = runTestApplication(
        externalServicesCfg = {
            hosts(KontaktinfoClientImpl.ingress) {
                routing {
                    get("/api/serviceowner/organizations/3/officialcontacts") {
                        call.respondText(flereEposterResponse, ContentType.Application.Json)
                    }
                }
            }
        }
    ) {
        val kontaktinfo = KontaktinfoClientImpl(client, successMaskinportenTokenProvider).hentKontaktinfo("3")
        assertEquals(
            Kontaktinfo(
                telefonnumre = setOf("+4700112233"),
                eposter = setOf("foo@example.com", "foo3@example.com", "foo2@example.com"),
            ),
            kontaktinfo
        )
    }

    @Test
    fun organisasjonFinnesIkke() = runTestApplication(
        externalServicesCfg = {
            hosts(KontaktinfoClientImpl.ingress) {
                routing {
                    get("/api/serviceowner/organizations/{orgnr}/officialcontacts") {
                        call.respond(
                            status = HttpStatusCode.BadRequest,
                            "Invalid organization number: ${call.pathParameters["orgnr"]}"
                        )
                    }
                }
            }
        }
    ) {


        /* Hvis orgnr ikke finnes får man responsen:
         * HTTP/1.1 400 Invalid organization number: 0000000 */
        assertFailsWith<RuntimeException> {
            KontaktinfoClientImpl(client, successMaskinportenTokenProvider).hentKontaktinfo("1")
        }
    }
}

/* Hentet ved kall mot tt02.altinn.no */
//language=JSON
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
//language=JSON
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
//language=JSON
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
//language=JSON
private val ingenKontaktinfoResponse = """
    []
"""


/* Legger på 3 eposter på en gang fra altinns GUI */
//language=JSON
private val flereEposterResponse = """
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