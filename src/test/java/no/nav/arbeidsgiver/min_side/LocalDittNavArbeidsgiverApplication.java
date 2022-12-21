package no.nav.arbeidsgiver.min_side;

import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

@SpringBootApplication
@Import(TokenSupportJwtConfig.class)
@EnableCaching
@EnableMockOAuth2Server
@Profile("local")
public class LocalDittNavArbeidsgiverApplication {
    public static void main(String[] args) {
        SpringApplication.run(LocalDittNavArbeidsgiverApplication.class, args);
    }
}
