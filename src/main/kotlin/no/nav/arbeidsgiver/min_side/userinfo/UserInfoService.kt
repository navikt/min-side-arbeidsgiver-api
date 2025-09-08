package no.nav.arbeidsgiver.min_side.userinfo

import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilganger
import no.nav.arbeidsgiver.min_side.services.digisyfo.DigisyfoService
import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusService

class UserInfoService(
    private val altinnService: AltinnService,
    private val digisyfoService: DigisyfoService,
    private val refusjonStatusService: RefusjonStatusService,
) {
    suspend fun getUserInfoV3(authenticatedUserHolder: AuthenticatedUserHolder) = supervisorScope {
        val tilganger = async {
            runCatching {
                altinnService.hentAltinnTilganger(authenticatedUserHolder)
            }
        }

        val syfoVirksomheter = async {
            runCatching {
                digisyfoService.hentVirksomheterOgSykmeldte(authenticatedUserHolder)
            }
        }
        val refusjoner = async {
            runCatching {
                refusjonStatusService.statusoversikt(authenticatedUserHolder)
            }
        }
        UserInfoV3.from(tilganger.await(), syfoVirksomheter.await(), refusjoner.await())
    }
}

data class UserInfoV3(
    val altinnError: Boolean,
    val digisyfoError: Boolean,
    val organisasjoner: List<AltinnTilganger.AltinnTilgang>,
    val tilganger: Map<String, Collection<String>>,
    val digisyfoOrganisasjoner: List<DigisyfoService.VirksomhetOgAntallSykmeldte>,
    val refusjoner: List<RefusjonStatusService.Statusoversikt>,
) {
    companion object {
        fun from(
            tilgangerResult: Result<AltinnTilganger>,
            syfoResult: Result<List<DigisyfoService.VirksomhetOgAntallSykmeldte>>,
            refusjonerResult: Result<List<RefusjonStatusService.Statusoversikt>>
        ) = UserInfoV3(
            digisyfoError = syfoResult.isFailure,

            altinnError = tilgangerResult.fold(
                onSuccess = { it.isError || refusjonerResult.isFailure },
                onFailure = { true }
            ),

            organisasjoner = tilgangerResult.fold(
                onSuccess = { it.hierarki },
                onFailure = { emptyList() }
            ),

            digisyfoOrganisasjoner = syfoResult.fold(
                onSuccess = { it },
                onFailure = { emptyList() }
            ),

            refusjoner = refusjonerResult.fold(
                onSuccess = { it },
                onFailure = { emptyList() }
            ),

            tilganger = tilgangerResult.fold(
                onSuccess = { it.tilgangTilOrgNr },
                onFailure = { emptyMap() }
            ),
        )
    }
}