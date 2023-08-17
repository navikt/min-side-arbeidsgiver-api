package no.nav.arbeidsgiver.min_side.controller

import no.nav.arbeidsgiver.min_side.SecurityConfiguration
import no.nav.arbeidsgiver.min_side.services.unleash.FeatureToggleService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@MockBeans(
    MockBean(JwtDecoder::class),
)
@WebMvcTest(
    value = [FeatureToggleController::class, SecurityConfiguration::class],
    properties = ["server.servlet.context-path=/"]
)
class FeatureToggleControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var featureToggleService: FeatureToggleService

    @Test
    internal fun featureTogglesAreUnprotected() {
        `when`(featureToggleService.hentFeatureToggles(listOf("foo"))).thenReturn(mapOf("foo" to true))

        mockMvc
            .perform(get("/api/feature?feature=foo"))
            .andExpect(status().isOk)
    }

}