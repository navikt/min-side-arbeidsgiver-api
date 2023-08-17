package no.nav.arbeidsgiver.min_side.controller

import no.nav.arbeidsgiver.min_side.mockserver.MockServer
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert.assertEquals
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@MockBean(MockServer::class)
@SpringBootTest(
    properties = [
        "server.servlet.context-path=/",
        "spring.flyway.cleanDisabled=false",
    ]
)
@AutoConfigureMockMvc
@DirtiesContext
class StorageIntegrationTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean // the real jwt decoder is bypassed by SecurityMockMvcRequestPostProcessors.jwt
    lateinit var jwtDecoder: JwtDecoder

    @Autowired
    lateinit var flyway: Flyway

    @BeforeEach
    fun setup() {
        flyway.clean()
        flyway.migrate()
    }

    @Test
    fun `no filter, no content`() {
        mockMvc
            .perform(
                get("/api/storage/{key}", "lagret-filter")
                    .with(jwtWithPid("42"))
            )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `lagrer og henter storage`() {
        val storage = """
            {
                "foo": "bar"
            }
        """

        mockMvc
            .perform(
                put("/api/storage/{key}", "lagret-filter")
                    .with(jwtWithPid("42"))
                    .content(storage)
            )
            .andExpect(status().isOk)
            .andExpect(header().stringValues("version", "1"))
            .andReturn().response.contentAsString.also {
                assertEquals(storage, it, true)
            }

        mockMvc
            .perform(
                get("/api/storage/{key}", "lagret-filter")
                    .with(jwtWithPid("42"))
            )
            .andExpect(status().isOk)
            .andExpect(header().stringValues("version", "1"))
            .andReturn().response.contentAsString.also {
                assertEquals(storage, it, true)
            }

        mockMvc
            .perform(
                put("/api/storage/{key}", "lagret-filter")
                    .with(jwtWithPid("42"))
                    .content(storage)
            )
            .andExpect(status().isOk)
            .andExpect(header().stringValues("version", "2"))
            .andReturn().response.contentAsString.also {
                assertEquals(storage, it, true)
            }

        mockMvc
            .perform(
                get("/api/storage/{key}", "lagret-filter")
                    .with(jwtWithPid("42"))
            )
            .andExpect(status().isOk)
            .andExpect(header().stringValues("version", "2"))
            .andReturn().response.contentAsString.also {
                assertEquals(storage, it, true)
            }

        mockMvc
            .perform(
                put("/api/storage/{key}?version={version}", "lagret-filter", "42")
                    .with(jwtWithPid("42"))
                    .content(storage)
            )
            .andExpect(status().isConflict)
            .andExpect(header().stringValues("version", "2"))
            .andReturn().response.contentAsString.also {
                assertEquals(storage, it, true)
            }

        mockMvc
            .perform(
                delete("/api/storage/{key}?version={version}", "lagret-filter", "314")
                    .with(jwtWithPid("42"))
            )
            .andExpect(status().isConflict)
            .andExpect(header().stringValues("version", "2"))
            .andReturn().response.contentAsString.also {
                assertEquals(storage, it, true)
            }

        mockMvc
            .perform(
                delete("/api/storage/{key}", "lagret-filter")
                    .with(jwtWithPid("42"))
            )
            .andExpect(status().isOk)
            .andExpect(header().stringValues("version", "2"))
            .andReturn().response.contentAsString.also {
                assertEquals(storage, it, true)
            }

        mockMvc
            .perform(
                get("/api/storage/{key}", "lagret-filter")
                    .with(jwtWithPid("42"))
            )
            .andExpect(status().isNoContent)
    }

    private fun jwtWithPid(pid: String): SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor =
        SecurityMockMvcRequestPostProcessors
            .jwt()
            .jwt(Jwt("tokenvalue", null, null, mapOf("alg" to "foo"), mapOf("pid" to pid)))

}