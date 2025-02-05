package no.nav.arbeidsgiver.min_side.userinfo

import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilganger
import no.nav.arbeidsgiver.min_side.services.digisyfo.DigisyfoService
import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class UserInfoController(
    private val altinnService: AltinnService,
    private val digisyfoService: DigisyfoService,
    private val refusjonStatusService: RefusjonStatusService,
    private val authenticatedUserHolder: AuthenticatedUserHolder,
) {
    @GetMapping("/api/userInfo/v3")
    suspend fun getUserInfoV3() = supervisorScope {
        val tilganger = async {
            runCatching {
                altinnService.hentAltinnTilganger()
            }
        }

        val syfoVirksomheter = async {
            runCatching {
                digisyfoService.hentVirksomheterOgSykmeldteV3(authenticatedUserHolder.fnr)
            }
        }
        val refusjoner = async {
            runCatching {
                refusjonStatusService.statusoversikt(authenticatedUserHolder.fnr)
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
    val digisyfoOrganisasjoner: List<DigisyfoService.VirksomhetOgAntallSykmeldteV3>,
    val refusjoner: List<RefusjonStatusService.Statusoversikt>,
) {
    companion object {
        fun from(
            tilgangerResult: Result<AltinnTilganger>,
            syfoResult: Result<List<DigisyfoService.VirksomhetOgAntallSykmeldteV3>>,
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