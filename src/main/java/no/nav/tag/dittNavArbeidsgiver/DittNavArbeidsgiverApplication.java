package no.nav.tag.dittNavArbeidsgiver;

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import no.nav.tag.dittNavArbeidsgiver.services.tokenExchange.ClientAssertionTokenFactory;
import no.nav.tag.dittNavArbeidsgiver.services.tokenExchange.TokenExchangeClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableJwtTokenValidation(ignore = {
        "springfox.documentation.swagger.web.ApiResourceController",
        "org.springframework"
})
@EnableCaching
public class DittNavArbeidsgiverApplication {

    public static void main(String [] args) {
        SpringApplication.run(DittNavArbeidsgiverApplication.class, args);
    }

    public static final String APP_NAME = "srvditt-nav-arbeid";
}
