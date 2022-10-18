package no.nav.arbeidsgiver.min_side.controller;


import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import no.nav.security.token.support.core.jwt.JwtToken;
import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;

@Component
public class AuthenticatedUserHolder {
    public static final String LOGINSERVICE = "selvbetjening";
    public static final String TOKENX = "tokenx";
    public static final String REQUIRED_LOGIN_LEVEL = "acr=Level4";

    private final TokenValidationContextHolder requestContextHolder;

    public AuthenticatedUserHolder(TokenValidationContextHolder requestContextHolder) {
        this.requestContextHolder = requestContextHolder;
    }

    public String getFnr() {
        return getJwtToken().getJwtTokenClaims().getStringClaim("pid");
    }

    public String getToken() {
        JwtToken jwtToken = getJwtToken();
        return jwtToken != null ? jwtToken.getTokenAsString() : null;
    }

    private JwtToken getJwtToken() {
        return requestContextHolder.getTokenValidationContext()
                .getFirstValidToken()
                .orElseThrow(() -> new NoSuchElementException(
                        "no valid token. how did you get so far without a valid token?"
                ));
    }
}
