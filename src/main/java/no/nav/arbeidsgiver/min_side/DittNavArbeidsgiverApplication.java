package no.nav.arbeidsgiver.min_side;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(TokenSupportJwtConfig.class)
@EnableCaching
public class DittNavArbeidsgiverApplication {

    public static void main(String[] args) {
        SpringApplication.run(DittNavArbeidsgiverApplication.class, args);
    }

    public static final String APP_NAME = "srvditt-nav-arbeid";
}
