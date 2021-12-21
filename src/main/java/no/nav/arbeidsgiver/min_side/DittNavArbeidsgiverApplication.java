package no.nav.arbeidsgiver.min_side;

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableJwtTokenValidation(ignore = {
        "org.springdoc",
        "org.springframework"
})
@EnableCaching
public class DittNavArbeidsgiverApplication {

    public static void main(String[] args) {
        SpringApplication.run(DittNavArbeidsgiverApplication.class, args);
    }

    public static final String APP_NAME = "srvditt-nav-arbeid";
}
