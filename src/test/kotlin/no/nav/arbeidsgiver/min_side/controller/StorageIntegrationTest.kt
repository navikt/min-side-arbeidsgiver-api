package no.nav.arbeidsgiver.min_side.controller

import no.nav.arbeidsgiver.min_side.mockserver.MockServer
import no.nav.arbeidsgiver.min_side.services.tokenExchange.ClientAssertionTokenFactory
import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONAssert.assertEquals
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@MockBean(MockServer::class)
@SpringBootTest(
    properties = [
        "server.servlet.context-path=/",
        "spring.flyway.cleanDisabled=false",
        "tokensupport.enabled=false",
    ]
)
@AutoConfigureMockMvc
@DirtiesContext
class StorageIntegrationTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var authenticatedUserHolder: AuthenticatedUserHolder

    @Autowired
    lateinit var flyway: Flyway

    @BeforeEach
    fun clearDatabase() {
        flyway.clean()
        flyway.migrate()
    }

    @Test
    fun `no filter, no content`() {
        `when`(authenticatedUserHolder.fnr).thenReturn("42")

        mockMvc
            .perform(get("/api/storage/{key}", "lagret-filter").accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isNoContent)
    }

    @Test
    fun `lagrer og henter storage`() {
        `when`(authenticatedUserHolder.fnr).thenReturn("42")
        val storage = """
            {
                "foo": "bar"
            }
        """

        mockMvc.perform {
            put("/api/storage/{key}", "lagret-filter").content(storage).buildRequest(it)
        }.andExpect(status().isOk)

        mockMvc
            .perform(get("/api/storage/{key}", "lagret-filter").accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(header().stringValues("version", "1"))
            .andReturn().response.contentAsString.also {
                assertEquals(storage, it, true)
            }

        mockMvc.perform {
            put("/api/storage/{key}", "lagret-filter").content(storage).buildRequest(it)
        }.andExpect(status().isOk)

        mockMvc
            .perform(get("/api/storage/{key}", "lagret-filter").accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(header().stringValues("version", "2"))
            .andReturn().response.contentAsString.also {
                assertEquals(storage, it, true)
            }

        mockMvc.perform {
            put("/api/storage/{key}?version={version}", "lagret-filter", "42")
                .content(storage)
                .buildRequest(it)
        }.andExpect(status().isConflict)

        mockMvc.perform {
            delete("/api/storage/{key}?version={version}", "lagret-filter", "42")
                .buildRequest(it)
        }.andExpect(status().isConflict)

        mockMvc.perform {
            delete("/api/storage/{key}", "lagret-filter")
                .buildRequest(it)
        }.andExpect(status().isOk)

        mockMvc
            .perform(get("/api/storage/{key}", "lagret-filter").accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isNoContent)
    }

}