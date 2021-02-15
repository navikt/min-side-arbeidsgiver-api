package no.nav.tag.dittNavArbeidsgiver.services.tokenExchange;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class ClientAssertionTokenFactory {

    private RSAKey tokenXPrivateJWK;
    private String clientId;
    private String tokendingsUrl;
    private JWSSigner signer;

    public ClientAssertionTokenFactory(@Value("${tokenX.privateJwk}") String tokenXPrivateJWK,
                                       @Value("${tokenX.clientId}") String clientId,
                                       @Value("${tokenX.tokendingsUrl}") String tokendingsUrl) throws ParseException {

        this.tokenXPrivateJWK = RSAKey.parse(tokenXPrivateJWK);
        this.clientId = clientId;
        this.tokendingsUrl = tokendingsUrl;
        try {
            this.signer = new RSASSASigner(this.tokenXPrivateJWK);
        }catch(JOSEException e){
            log.error("joseException", e);
        }

    }

    public String getClientAssertion (){
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(clientId)
                .issuer(clientId)
                .audience(tokendingsUrl)
                .issueTime(new Date() )
                .notBeforeTime(new Date() )
                .expirationTime(new Date(new Date().getTime() + 120 * 1000))
                .jwtID(UUID.randomUUID().toString())
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(tokenXPrivateJWK.getKeyID()).build(),
                claimsSet);
        try {
            signedJWT.sign(signer);
        }catch(JOSEException e){

        }
        return signedJWT.serialize();
    }



}
