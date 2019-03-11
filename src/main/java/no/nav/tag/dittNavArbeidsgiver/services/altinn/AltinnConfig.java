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
    private String altinnurl;
    private String APIGwHeader;

    public String getAltinnHeader() {
        return altinnHeader;
    }

    public void setAltinnHeader(String altinnHeader) {
        this.altinnHeader = altinnHeader;
    }

    public String getAltinnurl() {
        return altinnurl;
    }

    public void setAltinnurl(String altinnurl) {
        this.altinnurl = altinnurl;
    }

    public String getAPIGwHeader() {
        return APIGwHeader;
    }

    public void setAPIGwHeader(String APIGwHeader) {
        this.APIGwHeader = APIGwHeader;
    }

}
