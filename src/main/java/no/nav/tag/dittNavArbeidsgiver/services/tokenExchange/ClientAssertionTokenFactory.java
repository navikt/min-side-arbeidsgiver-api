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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class TokenExchangeClient {

    private RSAKey tokenXPrivetJWK;
    private String clientID;
    private String tokendingsUrl;
    private JWSSigner signer;

    @Autowired
    public TokenExchangeClient(@Value("${tokenX.privateJwk}") String tokenXPrivateJWK,
                     @Value("${tokenX.clientID}") String clientID,
    @Value("${tokenX.tokendingsUrl}") String tokendingsUrl) throws ParseException {


        this.tokenXPrivetJWK = RSAKey.parse(tokenXPrivateJWK);
        this.clientID = clientID;
        this.tokendingsUrl = tokendingsUrl;
        try {
            this.signer = new RSASSASigner(tokenXPrivetJWK);
        }catch(JOSEException e){
            log.error("joseException", e);
        }


    }



    public String getClientAssertion (){
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(clientID)
                .issuer(clientID)
                .audience(tokendingsUrl)
                .issueTime(new Date() )
                .notBeforeTime(new Date() )
                .expirationTime(new Date(new Date().getTime() + 120 * 1000))
                .jwtID(UUID.randomUUID().toString())
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(tokenXPrivetJWK.getKeyID()).build(),
                claimsSet);
        try {
            signedJWT.sign(signer);
        }catch(JOSEException e){

        }
        return signedJWT.serialize();
    }



}
