package no.nav.arbeidsgiver.min_side.userinfo

import com.fasterxml.jackson.annotation.JsonIgnore
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder
import no.nav.arbeidsgiver.min_side.models.Organisasjon
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
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

    /**
     * konseptet tjeneste id er noe som finnes i frontend.
     * på sikt bør oversetting/oppslag flyttes dit og denne koden slettes
     */
    val idLookup = mapOf(
        "5384:1" to "ekspertbistand",
        "4936:1" to "inntektsmelding",
        "4826:1" to "utsendtArbeidstakerEØS",
        "2896:87" to "endreBankkontonummerForRefusjoner",
        "5332:1" to "arbeidstrening",
        "5332:2" to "arbeidstrening",
        "5441:1" to "arbeidsforhold",
        "5516:1" to "midlertidigLønnstilskudd",
        "5516:2" to "varigLønnstilskudd",
        "5516:3" to "sommerjobb",
        "5516:4" to "mentortilskudd",
        "5516:5" to "inkluderingstilskudd",
        "3403:1" to "sykefravarstatistikk",
        "3403:2" to "sykefravarstatistikk",
        "5934:1" to "forebyggefravar",
        "5078:1" to "rekruttering",
        "5278:1" to "tilskuddsbrev",
        "5902:1" to "yrkesskade",
    )


    @GetMapping("/api/userInfo/v1")
    suspend fun getUserInfo(): UserInfoRespons {
        val (tilganger, organisasjoner, syfoVirksomheter, refusjoner) = supervisorScope {
            val tilganger = async {
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
                                id = idLookup[tilgang] ?: tilgang,
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
                                id = tilgang,
                                tjenestekode = tilgang,
                                tjenesteversjon = "",
                                organisasjoner = value.toList(),
                                altinnError = it.isError,
                            )
                        }
                    }
                }
            }

            val organisasjoner = async {
                runCatching {
                    altinnService.hentOrganisasjoner()
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
            altinnError = organisasjoner.isFailure || tilganger.any { it.altinnError } || refusjoner.isFailure,
            digisyfoError = syfoVirksomheter.isFailure,
            organisasjoner = organisasjoner.getOrDefault(emptyList()),
            digisyfoOrganisasjoner = syfoVirksomheter.getOrDefault(emptyList()),
            refusjoner = refusjoner.getOrDefault(emptyList()),
            tilganger = tilganger,
        )
    }

    data class UserInfoData(
        val tilganger: List<UserInfoRespons.Tilgang>,
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
            val id: String,
            val tjenestekode: String,
            val tjenesteversjon: String,
            val organisasjoner: List<String>,
            @get:JsonIgnore val altinnError: Boolean,
        )
    }

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
