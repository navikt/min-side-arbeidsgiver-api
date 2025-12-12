package no.nav.arbeidsgiver.min_side.services.kontaktinfo

import kotlinx.serialization.Serializable
import no.nav.arbeidsgiver.min_side.services.ereg.EregClient
import no.nav.arbeidsgiver.min_side.services.ereg.EregOrganisasjon.Companion.orgnummerTilOverenhet
import no.nav.arbeidsgiver.min_side.tilgangsstyring.AltinnRollerClient


class KontaktInfoService(
    private val altinnRollerClient: AltinnRollerClient,
    private val eregClient: EregClient,
    private val kontaktinfoClient: KontaktinfoClient,
) {
    suspend fun getKontaktinfo(requestBody: KontaktinfoRequest, fnr: String): KontaktinfoResponse {
        val orgnrUnderenhet = requestBody.virksomhetsnummer
        val orgnrHovedenhet = eregClient.hentOrganisasjon(orgnrUnderenhet)
            ?.orgnummerTilOverenhet()

        return KontaktinfoResponse(
            underenhet = tilgangsstyrOgHentKontaktinfo(orgnrUnderenhet, fnr),
            hovedenhet = orgnrHovedenhet?.let { tilgangsstyrOgHentKontaktinfo(it, fnr) },
        )
    }

    private suspend fun tilgangsstyrOgHentKontaktinfo(orgnr: String, fnr: String): Kontaktinfo? {
        val tilgangHovedenhet = altinnRollerClient.harAltinnRolle(
            fnr = fnr,
            orgnr = orgnr,
            altinnRoller = ALTINN_ROLLER,
            externalRoller = EXTERNAL_ROLLER,
        )

        return if (tilgangHovedenhet) {
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
        private val ALTINN_ROLLER = setOf(
            "HADM", // Hovedadministrator
            "SIGNE", // Signerer av Samordnet registermelding
        )
        private val EXTERNAL_ROLLER = setOf(
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

