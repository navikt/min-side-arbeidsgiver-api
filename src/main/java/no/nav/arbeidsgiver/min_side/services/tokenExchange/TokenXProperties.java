package no.nav.arbeidsgiver.min_side.services.tokenExchange;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.text.ParseException;

@Setter
@Profile({"local","dev-gcp","prod-gcp"})
@Configuration
@ConfigurationProperties("token.x")
@Slf4j
public class TokenXProperties implements InitializingBean {

    static final String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:token-exchange";
    static final String CLIENT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
    static final String SUBJECT_TOKEN_TYPE = "urn:ietf:params:oauth:token-type:jwt";

    String clientId;
    String issuer;
    String privateJwk;

    @Getter(lazy=true) private final RSAKey privateJwkRsa = parsePrivateJwk();
    @Getter(lazy=true) private final JWSSigner jwsSigner = createJWSSigner();

    public RSAKey parsePrivateJwk() {
        try {
            return RSAKey.parse(privateJwk);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public JWSSigner createJWSSigner() {
        try {
            return new RSASSASigner(getPrivateJwkRsa());
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("TokenX configured with issuer={} and clientId={}", issuer, clientId);
    }
}