package no.nav.arbeidsgiver.min_side.utils;


import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import org.springframework.stereotype.Component;

import static no.nav.arbeidsgiver.min_side.utils.FnrExtractor.ISSUER_SELVBETJENING;

@Component
public class TokenUtils {
    public static final String ISSUER = "selvbetjening";
    public static final String REQUIRED_LOGIN_LEVEL = "acr=Level4";

    private final TokenValidationContextHolder requestContextHolder;

    public TokenUtils(TokenValidationContextHolder requestContextHolder) {
        this.requestContextHolder = requestContextHolder;
    }

    public String getTokenForInnloggetBruker() {
        return requestContextHolder.getTokenValidationContext().getJwtToken(ISSUER_SELVBETJENING).getTokenAsString();
    }

}
