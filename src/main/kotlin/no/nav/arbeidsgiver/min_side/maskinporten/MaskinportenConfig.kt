package no.nav.arbeidsgiver.min_side.maskinporten

import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey


data class MaskinportenConfig2 (
    val scopes: String,
    val wellKnownUrl: String,
    val clientId: String,
    val clientJwk: String,
) {
    val privateJwkRsa: RSAKey = RSAKey.parse(clientJwk)
    val jwsSigner = RSASSASigner(privateJwkRsa)
}