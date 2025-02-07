package no.nav.arbeidsgiver.min_side.controller

import no.nav.arbeidsgiver.min_side.controller.SecurityMockMvcUtil.Companion.jwtWithPid
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert.assertEquals
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(
    properties = [
        "server.servlet.context-path=/",
        "spring.flyway.cleanDisabled=false",
    ]
)
@AutoConfigureMockMvc
class StorageIntegrationTest {
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

}