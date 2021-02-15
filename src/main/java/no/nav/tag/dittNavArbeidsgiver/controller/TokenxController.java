package no.nav.tag.dittNavArbeidsgiver.controller;

import no.nav.security.token.support.core.api.ProtectedWithClaims;
import no.nav.tag.dittNavArbeidsgiver.services.tokenExchange.TokenExchangeClient;
import no.nav.tag.dittNavArbeidsgiver.utils.TokenUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static no.nav.tag.dittNavArbeidsgiver.utils.TokenUtils.ISSUER;
import static no.nav.tag.dittNavArbeidsgiver.utils.TokenUtils.REQUIRED_LOGIN_LEVEL;

@ProtectedWithClaims(issuer=ISSUER, claimMap={REQUIRED_LOGIN_LEVEL})
@Profile({"local","dev-gcp"})
@RestController
public class TokenxController {

    private TokenExchangeClient tokenExchangeClient;
    private final TokenUtils tokenUtils;

    TokenxController(TokenExchangeClient tokenExchangeClient, TokenUtils tokenUtils){
        this.tokenExchangeClient = tokenExchangeClient;
        this.tokenUtils = tokenUtils;
    }

    @GetMapping("api/exchange-token")
    public String getTokenExchangeToken(){
        return tokenExchangeClient.exchangeToken(tokenUtils.getTokenForInnloggetBruker());
    }

}
