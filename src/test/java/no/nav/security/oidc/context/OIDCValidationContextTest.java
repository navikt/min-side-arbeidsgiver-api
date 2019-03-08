package no.nav.security.oidc.context;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import no.nav.security.oidc.context.OIDCClaims;
import no.nav.security.oidc.context.OIDCValidationContext;
import no.nav.security.oidc.context.TokenContext;
import org.junit.Test;

public class OIDCValidationContextTest {

    @Test
    public void getFirstValidToken() {
        OIDCValidationContext oidcValidationContext = new OIDCValidationContext();
        addValidatedToken("issuer2", oidcValidationContext);
        addValidatedToken("issuer1", oidcValidationContext);

        System.out.println(oidcValidationContext.getFirstValidToken());
    }

    private OIDCValidationContext addValidatedToken(String issuer, OIDCValidationContext oidcValidationContext) {
        oidcValidationContext.addValidatedToken(issuer, new TokenContext(issuer,
                "tokenstring"), new OIDCClaims(new PlainJWT(new JWTClaimsSet.Builder().build())));
        return oidcValidationContext;
    }
}