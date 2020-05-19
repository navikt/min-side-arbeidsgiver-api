package no.nav.tag.dittNavArbeidsgiver.utils;

import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.security.oidc.context.TokenContext;
import org.springframework.stereotype.Component;

import static no.nav.tag.dittNavArbeidsgiver.utils.FnrExtractor.ISSUER_SELVBETJENING;

@Component
public class TokenUtils {
    private final OIDCRequestContextHolder requestContextHolder;

    public TokenUtils(OIDCRequestContextHolder requestContextHolder) {
        this.requestContextHolder = requestContextHolder;
    }

    public String getTokenForInnloggetBruker() {
        return requestContextHolder.getOIDCValidationContext().getToken(ISSUER_SELVBETJENING).getIdToken();
    }

    public TokenContext getSelvbetjeningTokenContext() {
        return requestContextHolder.getOIDCValidationContext().getToken(ISSUER_SELVBETJENING);
    }

}
