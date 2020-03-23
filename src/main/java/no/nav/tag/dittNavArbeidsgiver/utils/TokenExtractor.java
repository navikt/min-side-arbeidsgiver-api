package no.nav.tag.dittNavArbeidsgiver.utils;

import no.nav.security.oidc.context.OIDCRequestContextHolder;

import static no.nav.tag.dittNavArbeidsgiver.utils.FnrExtractor.ISSUER_SELVBETJENING;

public class TokenExtractor {

    public static String extract(OIDCRequestContextHolder ctxHolder) {
        return ctxHolder.getOIDCValidationContext().getToken(ISSUER_SELVBETJENING).getIdToken();
    }
}
