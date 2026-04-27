package no.nav.arbeidsgiver.min_side

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilganger
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerService
import no.nav.arbeidsgiver.min_side.services.altinn.RessursMetadata
import no.nav.arbeidsgiver.min_side.services.altinn.rolleVisningsnavn
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("no.nav.arbeidsgiver.min_side.AltinnTilgangerRoutes")

suspend fun Application.configureAltinnTilgangerRoutes() {
    val altinnTilgangerService = dependencies.resolve<AltinnTilgangerService>()

    msaApiRouting {
        post("altinn-tilganger") {
            val tilganger = altinnTilgangerService.hentAltinnTilganger(subjectToken)
            val ressursMetadata = try {
                altinnTilgangerService.hentRessursMetadata()
            } catch (e: Exception) {
                log.warn("Klarte ikke hente ressursmetadata", e)
                emptyMap()
            }
            call.respond(AltinnTilgangerResponse.from(tilganger, ressursMetadata))
        }
    }
}

@Serializable
data class AltinnTilgangerResponse(
    val isError: Boolean,
    val hierarki: List<AltinnTilgangResponse>,
    val ressursMetadata: Map<String, RessursMetadata>,
) {
    companion object {
        fun from(
            altinnTilganger: AltinnTilganger,
            ressursMetadata: Map<String, RessursMetadata> = emptyMap(),
        ): AltinnTilgangerResponse {
            val brukerensRessurser = altinnTilganger.tilgangTilOrgNr.keys
                .filter { it.startsWith("nav_") }
                .toSet()
            return AltinnTilgangerResponse(
                isError = altinnTilganger.isError,
                hierarki = altinnTilganger.hierarki.map(AltinnTilgangResponse::from),
                ressursMetadata = ressursMetadata.filterKeys { it in brukerensRessurser },
            )
        }
    }
}

@Serializable
data class AltinnTilgangResponse(
    val orgnr: String,
    val navn: String,
    val organisasjonsform: String,
    val altinn3Tilganger: Set<String>,
    val roller: Set<AltinnRolleResponse>,
    val tilgangspakker: Set<String>,
    val underenheter: List<AltinnTilgangResponse>,
) {
    companion object {
        fun from(altinnTilgang: AltinnTilganger.AltinnTilgang): AltinnTilgangResponse = AltinnTilgangResponse(
            orgnr = altinnTilgang.orgnr,
            navn = altinnTilgang.navn,
            organisasjonsform = altinnTilgang.organisasjonsform,
            altinn3Tilganger = altinnTilgang.altinn3Tilganger.filter { it.startsWith("nav_") }.toSet(),
            roller = altinnTilgang.roller.map { rolle ->
                AltinnRolleResponse(
                    kode = rolle,
                    visningsnavn = rolleVisningsnavn[rolle] ?: rolle,
                )
            }.toSet(),
            tilgangspakker = altinnTilgang.tilgangspakker,
            underenheter = altinnTilgang.underenheter.map { underenhet -> from(underenhet) },
        )
    }
}

@Serializable
data class AltinnRolleResponse(
    val kode: String,
    val visningsnavn: String,
)
