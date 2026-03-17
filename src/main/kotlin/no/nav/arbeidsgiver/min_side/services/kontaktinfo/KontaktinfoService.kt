package no.nav.arbeidsgiver.min_side.services.kontaktinfo

import kotlinx.serialization.Serializable
import no.nav.arbeidsgiver.min_side.services.ereg.EregClient
import no.nav.arbeidsgiver.min_side.services.ereg.EregOrganisasjon.Companion.orgnummerTilOverenhet
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilgangerService


class KontaktInfoService(
    private val altinnTilgangerService: AltinnTilgangerService,
    private val eregClient: EregClient,
    private val kontaktinfoClient: KontaktinfoClient,
) {
    suspend fun getKontaktinfo(requestBody: KontaktinfoRequest, fnr: String, token: String): KontaktinfoResponse {
        val orgnrUnderenhet = requestBody.virksomhetsnummer
        val orgnrHovedenhet = eregClient.hentOrganisasjon(orgnrUnderenhet)
            ?.orgnummerTilOverenhet()

        return KontaktinfoResponse(
            underenhet = tilgangsstyrOgHentKontaktinfo(orgnrUnderenhet, token),
            hovedenhet = orgnrHovedenhet?.let { tilgangsstyrOgHentKontaktinfo(it, token) },
        )
    }

    private suspend fun tilgangsstyrOgHentKontaktinfo(orgnr: String, token: String): Kontaktinfo? {
        val harRolle = ROLLER.any { rolle ->
            altinnTilgangerService.harRolle(orgnr, rolle, token)
        }

        return if (harRolle) {
            kontaktinfoClient.hentKontaktinfo(orgnr).let {
                Kontaktinfo(
                    eposter = it.eposter.toList(),
                    telefonnumre = it.telefonnumre.toList(),
                )
            }
        } else {
            null
        }
    }

    @Serializable
    class KontaktinfoRequest(
        val virksomhetsnummer: String,
    ) {
        init {
            require(virksomhetsnummer.matches(orgnrRegex))
        }

        companion object {
            private val orgnrRegex = Regex("^[0-9]{9}$")
        }
    }

    @Suppress("unused") // DTO
    @Serializable
    class Kontaktinfo(
        val eposter: List<String>,
        val telefonnumre: List<String>,
    )

    @Suppress("unused") // DTO
    @Serializable
    class KontaktinfoResponse(
        /* null hvis ingen tilgang */
        val hovedenhet: Kontaktinfo?,

        /* null hvis ingen tilgang */
        val underenhet: Kontaktinfo?,
    )

    companion object {
        private val ROLLER = setOf(
            "HADM", // Hovedadministrator
            "SIGNE", // Signerer av Samordnet registermelding
            "DAGL", // daglig leder
            "LEDE", // styreleder
            "NEST", // nestleder
            "MEDL", // styremedlem
            "INNH", // innehaver
            "BOBE", // bobestyrer
            "BEST", // bestyrende reder
            "REPR", // norsk representant for utenlandske selskap
            "FFØR", // forretningsfører
            "KONT", // kontaktperson
        )
    }
}

