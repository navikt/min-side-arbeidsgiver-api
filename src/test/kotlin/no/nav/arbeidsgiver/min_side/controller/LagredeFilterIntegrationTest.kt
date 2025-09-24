package no.nav.arbeidsgiver.min_side.controller

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.arbeidsgiver.min_side.Database
import no.nav.arbeidsgiver.min_side.Database.Companion.openDatabase
import no.nav.arbeidsgiver.min_side.DatabaseConfig
import no.nav.arbeidsgiver.min_side.FakeApplication
import no.nav.arbeidsgiver.min_side.controller.SecurityMockMvcUtil.Companion.jwtWithPid
import no.nav.arbeidsgiver.min_side.defaultHttpClient
import no.nav.arbeidsgiver.min_side.services.lagredefilter.LagredeFilterService
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert.assertEquals
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


class LagredeFilterIntegrationTest {
    val testDatabaseConfig = DatabaseConfig(
        jdbcUrl = "jdbc:postgresql://localhost:2345/?password=postgres&user=postgres",
        migrationLocation = "db/migration"
    )

    val app = FakeApplication(8080) {
        provide<Database> { openDatabase(testDatabaseConfig) }
        provide<LagredeFilterService>(LagredeFilterService::class)
    }

    @BeforeEach
    fun setup() {
        app.start()
    }

    @AfterEach
    fun teardown() {
        app.stop()
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun fakeToken(pid: String): String {
        val header = """
            {
                "alg": "HS256",
                "typ": "JWT"
            }
        """.trimIndent()
        val payload = """
            {
              "active": true,
              "pid": "$pid",
              "acr": "idporten-loa-high"
            }
            """.trimIndent()
        val secret = "secret"

        fun String.b64Url(): String =
            Base64.UrlSafe.encode(this.encodeToByteArray()).trimEnd('=')

        val value = "${header.b64Url()}.${payload.b64Url()}.${secret.b64Url()}"
        return value
    }

    @Test
    fun `returnerer tomt array når bruker har ingen lagrede filter`() {
        runBlocking {
            val client = defaultHttpClient(configure = {
                install(DefaultRequest) {
                    url("http://localhost:8080")
                }
            })

            client.get("/api/lagredeFilter/") {
                bearerAuth(fakeToken("42"))
            }.let{
                assert(it.status == HttpStatusCode.OK)
                assert(it.bodyAsText() == "[]")
            }
        }


//        mockMvc
//            .perform(
//                get("/api/lagredeFilter/")
//                    .with(jwtWithPid("42"))
//            )
//            .andExpect(status().isOk)
//            .andReturn().response.contentAsString.also {
//                assertEquals("[]", it, true)
//            }
    }
//
//    @Test
//    fun `lagrer, returnerer, oppdaterer og sletter filter`() {
//        val filterJson = """
//                        {
//                            "filterId": "filter1",
//                            "navn": "uløste oppgaver",
//                            "side": 1,
//                            "tekstsoek": "",
//                            "virksomheter": [],
//                            "sortering": "NYESTE",
//                            "sakstyper": [],
//                            "oppgaveFilter": ["TILSTAND_NY"]
//                        }
//                    """.trimIndent()
//
//        val filterJson2 = """
//                        {
//                            "filterId": "filter2",
//                            "navn": " oppgaver med påminnelse for virksomhet 123456789",
//                            "side": 1,
//                            "tekstsoek": "",
//                            "virksomheter": ["123456789"],
//                            "sortering": "NYESTE",
//                            "sakstyper": [],
//                            "oppgaveFilter": ["TILSTAND_NY_MED_PÅMINNELSE"]
//                        }
//                    """.trimIndent()
//
//        mockMvc
//            .perform(
//                put("/api/lagredeFilter")
//                    .with(jwtWithPid("42"))
//                    .content(filterJson)
//                    .contentType(MediaType.APPLICATION_JSON)
//            )
//            .andExpect(status().isOk)
//
//
//        mockMvc
//            .perform(
//                put("/api/lagredeFilter")
//                    .with(jwtWithPid("42"))
//                    .content(filterJson2)
//                    .contentType(MediaType.APPLICATION_JSON)
//            )
//            .andExpect(status().isOk)
//
//        mockMvc
//            .perform(
//                get("/api/lagredeFilter")
//                    .with(jwtWithPid("42"))
//            )
//            .andExpect(status().isOk)
//            .andReturn().response.contentAsString.also {
//                assertEquals("[${filterJson},${filterJson2}]", it, true)
//            }
//
//        val oppdatertFilterJson2 = """
//                        {
//                            "filterId": "filter2",
//                            "navn": " oppgaver med påminnelse",
//                            "side": 1,
//                            "tekstsoek": "",
//                            "virksomheter": [],
//                            "sortering": "ELDSTE",
//                            "sakstyper": [],
//                            "oppgaveFilter": ["TILSTAND_NY_MED_PÅMINNELSE"]
//                        }
//                    """.trimIndent()
//
//        mockMvc
//            .perform(
//                put("/api/lagredeFilter")
//                    .with(jwtWithPid("42"))
//                    .content(oppdatertFilterJson2)
//                    .contentType(MediaType.APPLICATION_JSON)
//            )
//            .andExpect(status().isOk)
//
//        mockMvc
//            .perform(
//                get("/api/lagredeFilter")
//                    .with(jwtWithPid("42"))
//            )
//            .andExpect(status().isOk)
//            .andReturn().response.contentAsString.also {
//                assertEquals("[${filterJson},${oppdatertFilterJson2}]", it, true)
//            }
//
//        mockMvc
//            .perform(
//                delete("/api/lagredeFilter/filter1")
//                    .with(jwtWithPid("42"))
//            )
//            .andExpect(status().isOk)
//
//        mockMvc
//            .perform(
//                get("/api/lagredeFilter")
//                    .with(jwtWithPid("42"))
//            )
//            .andExpect(status().isOk)
//            .andReturn().response.contentAsString.also {
//                assertEquals("[${oppdatertFilterJson2}]", it, true)
//            }
//    }
}