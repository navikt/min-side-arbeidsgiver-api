package no.nav.arbeidsgiver.min_side.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.arbeidsgiver.min_side.models.Organisasjon
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import no.nav.arbeidsgiver.min_side.services.pushboks.PushboksRepository.Pushboks
import no.nav.arbeidsgiver.min_side.services.pushboks.PushboksService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(
    value = [PushboksController::class],
    properties = ["server.servlet.context-path=/", "tokensupport.enabled=false"]
)
class PushboksControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockBean
    lateinit var authenticatedUserHolder: AuthenticatedUserHolder

    @MockBean
    lateinit var altinnService: AltinnService

    @MockBean
    lateinit var pushboksService: PushboksService

    @Test
    fun hentPushbokser() {

        `when`(authenticatedUserHolder.fnr).thenReturn("42")
        `when`(
            altinnService.hentOrganisasjoner("42")
        ).thenReturn(
            listOf(
                Organisasjon(organizationNumber = "314"),
            )
        )
        `when`(pushboksService.hent("314")).thenReturn(
            listOf(
                Pushboks(
                    virksomhetsnummer = "314",
                    tjeneste = "foo",
                    innhold = objectMapper.convertValue(mapOf("a" to "b", "foo" to "bar"))
                )
            )
        )

        val jsonResponse = mockMvc
            .perform(get("/api/pushboks?virksomhetsnummer=314").accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk)
            .andReturn().response.contentAsString
        JSONAssert.assertEquals(
            """
            [
                {
                  "virksomhetsnummer": "314",
                  "tjeneste": "foo",
                  "innhold": {
                    "a": "b",
                    "foo": "bar"
                  }
                }
            ]
            """,
            jsonResponse,
            true
        )
    }

    @Test
    fun upsertBoks() {
        mockMvc
            .perform(
                put("/api/pushboks/foo/314")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{ "foo": "bar" }""")
            )
            .andDo(print())
            .andExpect(status().isOk)


        verify(pushboksService).upsert(
            tjeneste = "foo",
            virksomhetsnummer = "314",
            innhold = """{ "foo": "bar" }"""
        )
    }
}