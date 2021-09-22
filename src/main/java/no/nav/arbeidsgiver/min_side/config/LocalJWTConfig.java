package no.nav.arbeidsgiver.min_side.config;

import no.nav.security.token.support.test.spring.TokenGeneratorConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

@Configuration
@Import(TokenGeneratorConfiguration.class)
@Profile("local")
public class LocalJWTConfig {
}
