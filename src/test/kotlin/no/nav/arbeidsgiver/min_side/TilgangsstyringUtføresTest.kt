package no.nav.arbeidsgiver.min_side

import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.arbeidsgiver.min_side.infrastruktur.*
import kotlin.test.Test
import kotlin.test.assertEquals

class TilgangsstyringUtføresTest {

    @Test
    fun tilgangsstyringManglerToken() = runTestApplication(
        dependenciesCfg = {
            provide<TokenXTokenIntrospector> {
                MockTokenIntrospector {
                    if (it == "faketoken") mockIntrospectionResponse.withPid("42") else null
                }
            }
        },
        applicationCfg = {
            configureTokenXAuth()
            routing {
                authenticate(TOKENX_PROVIDER) {
                    get("/api/testauth") {
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
        }
    ) {
        client.get("/api/testauth") {
            expectSuccess = false
        }.let { response ->
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun tilgangsstyringErOkForAcrLevel4() = runTestApplication(
        dependenciesCfg = {
            provide<TokenXTokenIntrospector> {
                MockTokenIntrospector {
                    if (it == "faketokenLevel4") {
                        mockIntrospectionResponse.withPid("42").withAcr(ACR.LEVEL4)
                    } else null
                }
            }
        },
        applicationCfg = {
            configureTokenXAuth()
            routing {
                authenticate(TOKENX_PROVIDER) {
                    get("/api/testauth") {
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
        }
    ) {
        client.get("/api/testauth") {
            bearerAuth("faketokenLevel4")
        }.let { response ->
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    @Test
    fun tilgangsstyringErForbiddenForAndreAcrLevels() = runTestApplication(
        dependenciesCfg = {
            provide<TokenXTokenIntrospector> {
                MockTokenIntrospector {
                    if (it == "faketoken") {
                        mockIntrospectionResponse.withPid("42").withAcr("some-other-acr")
                    } else null
                }
            }
        },
        applicationCfg = {
            configureTokenXAuth()
            routing {
                authenticate(TOKENX_PROVIDER) {
                    get("/api/testauth") {
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
        }
    ) {
        client.get("/api/testauth") {
            bearerAuth("faketoken")
        }.let { response ->
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun tilgangsstyringErOkForAcridportenLoaHigh() = runTestApplication(
        dependenciesCfg = {
            provide<TokenXTokenIntrospector> {
                MockTokenIntrospector {
                    if (it == "faketokenLoaHigh") {
                        mockIntrospectionResponse.withPid("42").withAcr(ACR.IDPORTEN_LOA_HIGH)
                    } else null
                }
            }
        },
        applicationCfg = {
            configureTokenXAuth()
            routing {
                authenticate(TOKENX_PROVIDER) {
                    get("/api/testauth") {
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
        }
    ) {
        client.get("/api/testauth") {
            bearerAuth("faketokenLoaHigh")
        }.let { response ->
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }
}

