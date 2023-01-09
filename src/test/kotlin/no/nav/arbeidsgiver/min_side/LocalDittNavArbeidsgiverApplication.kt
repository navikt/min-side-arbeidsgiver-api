package no.nav.arbeidsgiver.min_side

import no.nav.arbeidsgiver.min_side.LocalDittNavArbeidsgiverApplication.MockOauthConfig
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.springframework.boot.SpringApplication
import org.springframework.context.annotation.Import

@Import(MockOauthConfig::class)
class LocalDittNavArbeidsgiverApplication : DittNavArbeidsgiverApplication() {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication(LocalDittNavArbeidsgiverApplication::class.java).apply {
                setAdditionalProfiles("local-kafka")
            }.run(*args)
        }
    }

    @EnableMockOAuth2Server
    class MockOauthConfig
}