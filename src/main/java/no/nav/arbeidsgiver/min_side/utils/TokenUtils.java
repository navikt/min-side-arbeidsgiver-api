package no.nav.arbeidsgiver.min_side.utils;


import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import no.nav.security.token.support.core.jwt.JwtToken;
import org.springframework.stereotype.Component;

@Component
public class TokenUtils {
    public static final String ISSUER = "selvbetjening";
    public static final String REQUIRED_LOGIN_LEVEL = "acr=Level4";

    private final TokenValidationContextHolder requestContextHolder;

    public TokenUtils(TokenValidationContextHolder requestContextHolder) {
        this.requestContextHolder = requestContextHolder;
    }

    public String getFnrForInnloggetBruker() {
        return getJwtToken().getJwtTokenClaims().getStringClaim("pid");
    }

    public String getTokenForInnloggetBruker() {
        return getJwtToken().getTokenAsString();
    }

    private JwtToken getJwtToken() {
        return requestContextHolder.getTokenValidationContext().getJwtToken(ISSUER);
    }

}
