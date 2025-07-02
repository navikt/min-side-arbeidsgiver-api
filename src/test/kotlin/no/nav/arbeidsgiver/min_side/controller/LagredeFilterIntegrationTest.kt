package no.nav.arbeidsgiver.min_side.controller

import no.nav.arbeidsgiver.min_side.controller.SecurityMockMvcUtil.Companion.jwtWithPid
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert.assertEquals
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(
    properties = [
        "server.servlet.context-path=/",
        "spring.flyway.cleanDisabled=false",
    ]
)
@AutoConfigureMockMvc
class LagredeFilterIntegrationTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean // the real jwt decoder is bypassed by SecurityMockMvcRequestPostProcessors.jwt
    lateinit var jwtDecoder: JwtDecoder

    @Autowired
    lateinit var flyway: Flyway

    @BeforeEach
    fun setup() {
        flyway.clean()
        flyway.migrate()
    }

    @Test
    fun `returnerer tomt array når bruker har ingen lagrede filter`() {
        mockMvc
            .perform(
                get("/api/lagredeFilter/")
                    .with(jwtWithPid("42"))
            )
            .andExpect(status().isOk)
            .andReturn().response.contentAsString.also {
                assertEquals("[]", it, true)
            }
    }

    @Test
    fun `lagrer, returnerer, oppdaterer og sletter filter`() {
        val filterJson = """
                        {
                            "filterId": "filter1",
                            "navn": "uløste oppgaver",
                            "side": 1,
                            "tekstsoek": "",
                            "virksomheter": [],
                            "sortering": "NYESTE_FØRST",
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
                            "sortering": "NYESTE_FØRST",
                            "sakstyper": [],
                            "oppgaveFilter": ["TILSTAND_NY_MED_PÅMINNELSE"] 
                        }
                    """.trimIndent()

        mockMvc
            .perform(
                put("/api/lagredeFilter")
                    .with(jwtWithPid("42"))
                    .content(filterJson)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk)


        mockMvc
            .perform(
                put("/api/lagredeFilter")
                    .with(jwtWithPid("42"))
                    .content(filterJson2)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk)

        mockMvc
            .perform(
                get("/api/lagredeFilter")
                    .with(jwtWithPid("42"))
            )
            .andExpect(status().isOk)
            .andReturn().response.contentAsString.also {
                assertEquals("[${filterJson},${filterJson2}]", it, true)
            }

        val oppdatertFilterJson2 = """
                        {
                            "filterId": "filter2",
                            "navn": " oppgaver med påminnelse",
                            "side": 1,
                            "tekstsoek": "",
                            "virksomheter": [],
                            "sortering": "ELDSTE_FØRST",
                            "sakstyper": [],
                            "oppgaveFilter": ["TILSTAND_NY_MED_PÅMINNELSE"] 
                        }
                    """.trimIndent()

        mockMvc
            .perform(
                put("/api/lagredeFilter")
                    .with(jwtWithPid("42"))
                    .content(oppdatertFilterJson2)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk)

        mockMvc
            .perform(
                get("/api/lagredeFilter")
                    .with(jwtWithPid("42"))
            )
            .andExpect(status().isOk)
            .andReturn().response.contentAsString.also {
                assertEquals("[${filterJson},${oppdatertFilterJson2}]", it, true)
            }

        mockMvc
            .perform(
                delete("/api/lagredeFilter/filter1")
                    .with(jwtWithPid("42"))
            )
            .andExpect(status().isOk)

        mockMvc
            .perform(
                get("/api/lagredeFilter")
                    .with(jwtWithPid("42"))
            )
            .andExpect(status().isOk)
            .andReturn().response.contentAsString.also {
                assertEquals("[${oppdatertFilterJson2}]", it, true)
            }
    }
}