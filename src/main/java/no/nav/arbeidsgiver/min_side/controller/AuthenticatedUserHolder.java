package no.nav.arbeidsgiver.min_side.controller;


import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import no.nav.security.token.support.core.jwt.JwtToken;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserHolder {
    public static final String ISSUER = "selvbetjening";
    public static final String REQUIRED_LOGIN_LEVEL = "acr=Level4";

    private final TokenValidationContextHolder requestContextHolder;

    public AuthenticatedUserHolder(TokenValidationContextHolder requestContextHolder) {
        this.requestContextHolder = requestContextHolder;
    }

    public String getFnr() {
        return getJwtToken().getJwtTokenClaims().getStringClaim("pid");
    }

    public String getToken() {
        return getJwtToken().getTokenAsString();
    }

    private JwtToken getJwtToken() {
        return requestContextHolder.getTokenValidationContext().getJwtToken(ISSUER);
    }

}
