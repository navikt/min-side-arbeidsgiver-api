package no.nav.arbeidsgiver.min_side;

import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Import;

@Import(LocalDittNavArbeidsgiverApplication.MockOauthConfig.class)
public class LocalDittNavArbeidsgiverApplication extends DittNavArbeidsgiverApplication {
    public static void main(String[] args) {
        SpringApplication.run(LocalDittNavArbeidsgiverApplication.class, args);
    }

    @EnableMockOAuth2Server
    public static class MockOauthConfig {

    }
}
