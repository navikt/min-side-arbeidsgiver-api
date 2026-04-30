package no.nav.arbeidsgiver.min_side

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.arbeidsgiver.min_side.services.altinn.AccessPackageArea
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilganger
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerService
import no.nav.arbeidsgiver.min_side.services.altinn.LocalizedText
import no.nav.arbeidsgiver.min_side.services.altinn.RessursMetadataResponse
import no.nav.arbeidsgiver.min_side.services.altinn.rolleVisningsnavn
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("no.nav.arbeidsgiver.min_side.AltinnTilgangerRoutes")

suspend fun Application.configureAltinnTilgangerRoutes() {
    val altinnTilgangerService = dependencies.resolve<AltinnTilgangerService>()

    msaApiRouting {
        post("altinn-tilganger") {
            val tilganger = altinnTilgangerService.hentAltinnTilganger(subjectToken)
            val ressursMetadataResponse = try {
                altinnTilgangerService.hentRessursMetadata()
            } catch (e: Exception) {
                log.warn("Klarte ikke hente ressursmetadata", e)
                RessursMetadataResponse(emptyMap())
            }
            call.respond(AltinnTilgangerResponse.from(tilganger, ressursMetadataResponse))
        }
    }
}

@Serializable
data class AltinnTilgangerResponse(
    val isError: Boolean,
    val hierarki: List<AltinnTilgangResponse>,
) {
    companion object {
        fun from(
            altinnTilganger: AltinnTilganger,
            ressursMetadataResponse: RessursMetadataResponse,
        ) = AltinnTilgangerResponse(
            isError = altinnTilganger.isError,
            hierarki = altinnTilganger.hierarki.map { AltinnTilgangResponse.from(it, ressursMetadataResponse) },
        )
    }
}

@Serializable
data class AltinnTilgangResponse(
    val orgnr: String,
    val navn: String,
    val organisasjonsform: String,
    val altinn3Tilganger: List<Altinn3TilgangResponse>,
    val roller: List<AltinnRolleResponse>,
    val tilgangspakker: List<TilgangspakkeResponse>,
    val underenheter: List<AltinnTilgangResponse>,
) {
    companion object {
        fun from(
            altinnTilgang: AltinnTilganger.AltinnTilgang,
            ressursMetadataResponse: RessursMetadataResponse,
        ): AltinnTilgangResponse {
            val roller = altinnTilgang.roller.map { rolle ->
                AltinnRolleResponse(kode = rolle, visningsnavn = rolleVisningsnavn[rolle] ?: rolle)
            }
            val tilgangspakker = altinnTilgang.tilgangspakker.map { id ->
                val metadata = ressursMetadataResponse.accessPackages[id]
                TilgangspakkeResponse(
                    id = id,
                    navn = metadata?.name ?: id,
                    beskrivelse = metadata?.description,
                    area = metadata?.area,
                )
            }
            val altinn3Tilganger = altinnTilgang.altinn3Tilganger
                .filter { it.startsWith("nav_") }
                .map { ressursId ->
                    val metadata = ressursMetadataResponse.resources[ressursId]
                    val delegertViaRoller = if (metadata != null) {
                        roller.filter { rolle ->
                            metadata.grantedByRoles.any { it.equals(rolle.kode, ignoreCase = true) }
                        }
                    } else {
                        emptyList()
                    }
                    val delegertViaTilgangspakker = if (metadata != null) {
                        tilgangspakker.filter { pakke ->
                            metadata.grantedByAccessPackages.any { it == pakke.id }
                        }
                    } else {
                        emptyList()
                    }
                    Altinn3TilgangResponse(
                        ressursId = ressursId,
                        navn = metadata?.metadata?.title,
                        beskrivelse = metadata?.metadata?.rightDescription,
                        delegertViaRoller = delegertViaRoller,
                        delegertViaTilgangspakker = delegertViaTilgangspakker,
                        erEnkeltrettighet = if (metadata != null) {
                            delegertViaRoller.isEmpty() && delegertViaTilgangspakker.isEmpty()
                        } else {
                            null
                        },
                    )
                }
            return AltinnTilgangResponse(
                orgnr = altinnTilgang.orgnr,
                navn = altinnTilgang.navn,
                organisasjonsform = altinnTilgang.organisasjonsform,
                altinn3Tilganger = altinn3Tilganger,
                roller = roller,
                tilgangspakker = tilgangspakker,
                underenheter = altinnTilgang.underenheter.map { from(it, ressursMetadataResponse) },
            )
        }
    }
}

@Serializable
data class Altinn3TilgangResponse(
    val ressursId: String,
    val navn: LocalizedText?,
    val beskrivelse: LocalizedText?,
    /** Roller brukeren har som gir denne rettigheten (basert på ressursmetadata). */
    val delegertViaRoller: List<AltinnRolleResponse>,
    /** Tilgangspakker brukeren har som gir denne rettigheten (basert på ressursmetadata). */
    val delegertViaTilgangspakker: List<TilgangspakkeResponse>,
    /** True dersom rettigheten ikke er utledet fra rolle eller tilgangspakke. Null dersom metadata mangler. */
    val erEnkeltrettighet: Boolean?,
)

@Serializable
data class TilgangspakkeResponse(
    val id: String,
    val navn: String,
    val beskrivelse: String? = null,
    val area: AccessPackageArea? = null,
)

@Serializable
data class AltinnRolleResponse(
    val kode: String,
    val visningsnavn: String,
)
