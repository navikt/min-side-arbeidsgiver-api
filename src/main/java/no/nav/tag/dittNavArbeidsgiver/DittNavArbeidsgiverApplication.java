package no.nav.tag.dittNavArbeidsgiver;

import no.nav.security.spring.oidc.api.EnableOIDCTokenValidation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableOIDCTokenValidation(ignore = { 
        "springfox.documentation.swagger.web.ApiResourceController",
        "org.springframework"
})
@EnableCaching
public class DittNavArbeidsgiverApplication {
    public static void main(String [] args) {
        SpringApplication.run(DittNavArbeidsgiverApplication.class, args);
    }
}
