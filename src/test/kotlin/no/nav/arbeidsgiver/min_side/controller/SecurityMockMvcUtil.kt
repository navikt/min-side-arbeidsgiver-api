package no.nav.arbeidsgiver.min_side.controller

import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt

class SecurityMockMvcUtil {
    companion object {
        fun jwtWithPid(
            pid: String,
            configure: Jwt.Builder.() -> Unit = {},
        ): JwtRequestPostProcessor =
            jwt().jwt { jwt ->
                jwt.claim("pid", pid)
                jwt.configure()
            }
    }
}