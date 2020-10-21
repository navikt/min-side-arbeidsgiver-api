package no.nav.tag.dittNavArbeidsgiver.config;

import no.nav.security.token.support.test.spring.TokenGeneratorConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

@Configuration
@Import(TokenGeneratorConfiguration.class)
@Profile("local")
public class LocalJWTConfig {
}
