package no.nav.arbeidsgiver.min_side.services.tokenExchange

interface TokenExchangeClient {
    fun exchange(subjectToken: String, audience: String): TokenXToken
}