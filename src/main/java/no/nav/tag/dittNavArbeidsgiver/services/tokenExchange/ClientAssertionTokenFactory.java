package no.nav.tag.dittNavArbeidsgiver.services.tokenExchange;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

@Slf4j
@Profile({"local", "dev-gcp","prod-gcp"})
@Component
public class ClientAssertionTokenFactory {

    final TokenXProperties properties;

    public ClientAssertionTokenFactory(TokenXProperties properties) {
        this.properties = properties;
    }

    public String getClientAssertion() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(properties.clientId)
                .issuer(properties.clientId)
                .audience(properties.tokendingsUrl)
                .issueTime(new Date())
                .notBeforeTime(new Date())
                .expirationTime(new Date(new Date().getTime() + 120 * 1000))
                .jwtID(UUID.randomUUID().toString())
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(properties.getPrivateJwkRsa().getKeyID())
                        .build(),
                claimsSet
        );

        try {
            signedJWT.sign(properties.getJwsSigner());
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
        return signedJWT.serialize();
    }


}
