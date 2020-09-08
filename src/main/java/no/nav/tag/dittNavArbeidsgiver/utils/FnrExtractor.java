package no.nav.tag.dittNavArbeidsgiver.utils;


import no.nav.security.token.support.core.context.TokenValidationContextHolder;

public class FnrExtractor {
    public static String ISSUER_SELVBETJENING = "selvbetjening";

    public static String extract(TokenValidationContextHolder ctxHolder) {
        return ctxHolder.getTokenValidationContext().getClaims(ISSUER_SELVBETJENING).getSubject();
    }
}
