package no.nav.tag.dittNavArbeidsgiver.services.altinn;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "altinn")
public class AltinnConfig {


    private String altinnHeader;
    private String altinnUrl;
    private String APIGwHeader;

    public String getAltinnUrl() {
        return this.altinnUrl;
    }

    public String getApiKey() {
        return this.altinnHeader;
    }

    public String getGatewayKey() {
        return this.APIGwHeader;
    }

}
