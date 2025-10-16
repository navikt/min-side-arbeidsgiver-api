package no.nav.arbeidsgiver.min_side.maskinporten

import no.nav.arbeidsgiver.min_side.config.Miljø
import no.nav.arbeidsgiver.notifikasjon.infrastruktur.texas.AuthClientImpl
import no.nav.arbeidsgiver.notifikasjon.infrastruktur.texas.IdentityProvider
import no.nav.arbeidsgiver.notifikasjon.infrastruktur.texas.TexasAuthConfig

interface MaskinportenClient {
    suspend fun fetchNewAccessToken(): String
}

class MaskinportenClientImpl(
    val config: MaskinportenConfig2,
) : MaskinportenClient {

    private val authClient = AuthClientImpl(
        config = TexasAuthConfig.nais(),
        provider = IdentityProvider.MASKINPORTEN
    )

    override suspend fun fetchNewAccessToken() = authClient.token(
            config.scopes,
            mapOf("resource" to Miljø.resolve(
                prod = { "https://www.altinn.no/" },
                other = { "https://tt02.altinn.no/" })
            )
        ).fold(
            onSuccess = {
                it.accessToken
            },
            onError = {
                throw Exception("Failed to fetch token: ${it.status} ${it.error}")
            }
        )
}

