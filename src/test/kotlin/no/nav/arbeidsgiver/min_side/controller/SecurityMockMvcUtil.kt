package no.nav.arbeidsgiver.min_side.controller

import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors

class SecurityMockMvcUtil {
    companion object {
        fun jwtWithPid(pid: String): SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor {
            return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt { jwt ->
                    jwt.claim("pid", pid)
                }
        }

    }
}