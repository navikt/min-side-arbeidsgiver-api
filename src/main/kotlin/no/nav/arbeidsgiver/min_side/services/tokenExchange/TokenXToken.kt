package no.nav.arbeidsgiver.min_side.services.tokenExchange


data class TokenXToken(
    var access_token: String? = null,
    var issued_token_type: String? = null,
    var token_type: String? = null,
    var expires_in: Int = 0
)