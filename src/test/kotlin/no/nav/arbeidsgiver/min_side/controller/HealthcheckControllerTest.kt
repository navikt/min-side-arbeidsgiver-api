package no.nav.arbeidsgiver.min_side.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(
    value = [HealthcheckController::class],
    properties = ["server.servlet.context-path=/", "tokensupport.enabled=false"]
)
class HealthcheckControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    internal fun isAlive() {
        mockMvc
            .perform(get("/internal/isAlive"))
            .andExpect(status().isOk)
    }

    @Test
    internal fun isReady() {
        mockMvc
            .perform(get("/internal/isReady"))
            .andExpect(status().isOk)
    }
}