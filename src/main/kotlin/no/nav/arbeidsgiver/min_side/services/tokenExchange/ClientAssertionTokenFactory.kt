package no.nav.arbeidsgiver.min_side.services.tokenExchange

import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.util.*

class ClientAssertionTokenFactory(val properties: TokenXProperties) {
    val clientAssertion: String
        get() {
            val claimsSet = JWTClaimsSet.Builder()
                .subject(properties.clientId)
                .issuer(properties.clientId)
                .audience(properties.tokenEndpoint)
                .issueTime(Date())
                .notBeforeTime(Date())
                .expirationTime(Date(Date().time + 120 * 1000))
                .jwtID(UUID.randomUUID().toString())
                .build()
            val signedJWT = SignedJWT(
                JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(properties.privateJwkRsa.keyID)
                    .build(),
                claimsSet
            )
            try {
                signedJWT.sign(properties.jwsSigner)
            } catch (e: JOSEException) {
                throw RuntimeException(e)
            }
            return signedJWT.serialize()
        }
}