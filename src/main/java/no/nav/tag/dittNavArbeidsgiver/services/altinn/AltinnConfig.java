package no.nav.tag.dittNavArbeidsgiver.services.altinn;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "altinn")
public class AltinnConfig {
    private String altinnHeader;
    private String altinnurl;
    private String APIGwHeader;

}
