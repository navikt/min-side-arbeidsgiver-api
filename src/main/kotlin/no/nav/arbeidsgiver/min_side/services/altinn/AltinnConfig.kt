package no.nav.arbeidsgiver.min_side.services.altinn;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "altinn")
public class AltinnConfig {
    private String altinnHeader;
    private String altinnurl;
    private String APIGwHeader;
    private String proxyUrl;
    private String proxyFallbackUrl;
    private String proxyAudience;
}
