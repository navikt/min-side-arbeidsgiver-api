package no.nav.arbeidsgiver.min_side.userinfo

import com.fasterxml.jackson.annotation.JsonIgnore
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder
import no.nav.arbeidsgiver.min_side.models.Organisasjon
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnTilganger
import no.nav.arbeidsgiver.min_side.services.digisyfo.DigisyfoService
import no.nav.arbeidsgiver.min_side.services.digisyfo.DigisyfoService.VirksomhetOgAntallSykmeldte
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

    @GetMapping("/api/userInfo/v1")
    suspend fun getUserInfo(): UserInfoRespons {
        val (tilganger, organisasjoner, syfoVirksomheter, refusjoner) = supervisorScope {
            val tilganger = async {
                runCatching {
                    altinnService.hentAltinnTilganger().let {
                        it.tilgangTilOrgNr.map { (tilgang, value) ->
                            if (tilgang.contains(":")) {
                                /**
                                 * I frontend mappes tilganger til en record fra tjeneste "id" til et set med orgnr
                                 * dette blir dobbeltarbeid. Endre frontend til å motta map direkte på form:
                                 * {
                                 *    "tjenestekode:tjenesteversjon": ["orgnr1", "orgnr2"]
                                 * }
                                 */
                                val (tjenestekode, tjenesteversjon) = tilgang.split(":")
                                UserInfoRespons.Tilgang(
                                    tjenestekode = tjenestekode,
                                    tjenesteversjon = tjenesteversjon,
                                    organisasjoner = value.toList(),
                                    altinnError = it.isError,
                                )
                            } else {
                                /**
                                 * altinn3 ressurser har ingen tjenesteversjon
                                 * her burde vi på sikt ha en bedre måte å skille på
                                 * altinn2 tjeneste tilgang vs altinn3 ressurs tilgang
                                 */
                                UserInfoRespons.Tilgang(
                                    tjenestekode = tilgang,
                                    tjenesteversjon = "",
                                    organisasjoner = value.toList(),
                                    altinnError = it.isError,
                                )
                            }
                        }
                    }
                }
            }

            val organisasjoner = async {
                runCatching {
                    altinnService.hentAltinnTilganger().organisasjonerFlattened
                }
            }

            val syfoVirksomheter = async {
                runCatching {
                    digisyfoService.hentVirksomheterOgSykmeldte(authenticatedUserHolder.fnr)
                }
            }

            val refusjoner = async {
                runCatching {
                    refusjonStatusService.statusoversikt(authenticatedUserHolder.fnr)
                }
            }


            UserInfoData(tilganger.await(), organisasjoner.await(), syfoVirksomheter.await(), refusjoner.await())
        }

        return UserInfoRespons(
            altinnError = organisasjoner.isFailure || (tilganger.isFailure || tilganger.getOrDefault(emptyList()).any { it.altinnError }) || refusjoner.isFailure,
            digisyfoError = syfoVirksomheter.isFailure,
            organisasjoner = organisasjoner.getOrDefault(emptyList()),
            digisyfoOrganisasjoner = syfoVirksomheter.getOrDefault(emptyList()),
            refusjoner = refusjoner.getOrDefault(emptyList()),
            tilganger = tilganger.getOrDefault(emptyList()),
        )
    }

    data class UserInfoData(
        val tilganger: Result<List<UserInfoRespons.Tilgang>>,
        val organisasjoner: Result<List<Organisasjon>>,
        val digisyfoOrganisasjoner: Result<Collection<VirksomhetOgAntallSykmeldte>>,
        val refusjoner: Result<List<RefusjonStatusService.Statusoversikt>>,
    )

    data class UserInfoRespons(
        val organisasjoner: List<Organisasjon>,
        val tilganger: List<Tilgang>,
        val digisyfoOrganisasjoner: Collection<VirksomhetOgAntallSykmeldte>,
        val altinnError: Boolean,
        val digisyfoError: Boolean,
        val refusjoner: List<RefusjonStatusService.Statusoversikt>,
    ) {
        data class Tilgang(
            val tjenestekode: String,
            val tjenesteversjon: String,
            val organisasjoner: List<String>,
            @get:JsonIgnore val altinnError: Boolean,
        )
    }


    @GetMapping("/api/userInfo/v2")
    suspend fun getUserInfoV2(): UserInfoV2Respons {
        val (tilganger, syfoVirksomheter, refusjoner) = supervisorScope {
            val tilganger = async {
                runCatching {
                    altinnService.hentAltinnTilganger()
                }
            }

            val syfoVirksomheter = async {
                runCatching {
                    digisyfoService.hentVirksomheterOgSykmeldte(authenticatedUserHolder.fnr)
                }
            }

            val refusjoner = async {
                runCatching {
                    refusjonStatusService.statusoversikt(authenticatedUserHolder.fnr)
                }
            }

            Triple(tilganger.await(), syfoVirksomheter.await(), refusjoner.await())
        }


        return UserInfoV2Respons(
            altinnError = tilganger.isFailure || tilganger.getOrNull()?.isError ?: false || refusjoner.isFailure,
            digisyfoError = syfoVirksomheter.isFailure,
            organisasjoner = tilganger.getOrNull()?.hierarki ?: emptyList(),
            digisyfoOrganisasjoner = syfoVirksomheter.getOrDefault(emptyList()),
            refusjoner = refusjoner.getOrDefault(emptyList()),
            tilganger = tilganger.getOrNull()?.tilgangTilOrgNr ?: emptyMap<String, Set<String>>(),
        )
    }

    data class UserInfoV2Respons(
        val altinnError: Boolean,
        val digisyfoError: Boolean,
        val organisasjoner: List<AltinnTilganger.AltinnTilgang>,
        val tilganger: Map<String, Collection<String>>,
        val digisyfoOrganisasjoner: Collection<VirksomhetOgAntallSykmeldte>,
        val refusjoner: List<RefusjonStatusService.Statusoversikt>,
    )

    sealed interface Tjeneste {
        val sort: Sort
        val tjenestekode: String
        val tjenesteversjon: String
    }

    data class NAVTjeneste(
        override val tjenestekode: String,
        override val tjenesteversjon: String,
    ) : Tjeneste {
        override val sort = Sort.tjeneste
    }

    data class Altinnskjema(
        override val tjenestekode: String,
        override val tjenesteversjon: String,
    ) : Tjeneste {
        override val sort = Sort.skjema
    }

    enum class Sort { tjeneste, skjema }
}
