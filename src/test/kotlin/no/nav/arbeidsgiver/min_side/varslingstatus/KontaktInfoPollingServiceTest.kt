package no.nav.arbeidsgiver.min_side.varslingstatus

import no.nav.arbeidsgiver.min_side.infrastruktur.Database
import no.nav.arbeidsgiver.min_side.infrastruktur.resolve
import no.nav.arbeidsgiver.min_side.infrastruktur.runTestApplicationWithDatabase
import no.nav.arbeidsgiver.min_side.services.ereg.*
import no.nav.arbeidsgiver.min_side.services.kontaktinfo.KontaktinfoClient
import no.nav.arbeidsgiver.min_side.services.kontaktinfo.KontaktinfoClient.Kontaktinfo
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class KontaktInfoPollingServiceTest {

    val underenhetOrgnr = "1".repeat(9)

    val hovedenhetOrgnr = "2".repeat(9)

    val underenhet = EregOrganisasjon(
        organisasjonsnummer = underenhetOrgnr,
        organisasjonDetaljer = EregOrganisasjonDetaljer(
            ansatte = null,
            naeringer = null,
            enhetstyper = listOf(EregEnhetstype("BEDR", null)),
            postadresser = null,
            forretningsadresser = null,
            internettadresser = null
        ),
        inngaarIJuridiskEnheter = listOf(
            EregEnhetsRelasjon(
                hovedenhetOrgnr, null
            )
        ),
        bestaarAvOrganisasjonsledd = null,
        type = "virksomhet",
        navn = EregNavn("organisasjon", null)
    )

    val ingenKontaktinfo = Kontaktinfo(
        eposter = setOf(),
        telefonnumre = setOf(),
    )

    val kontaktinfoMedEpost = Kontaktinfo(
        eposter = setOf("post@example.com"),
        telefonnumre = setOf(),
    )

    val kontaktinfoMedTlf = Kontaktinfo(
        eposter = setOf(),
        telefonnumre = setOf("00000000"),
    )

    private fun mockKontaktinfoClient(kontaktinfo: Map<String, Kontaktinfo>): KontaktinfoClient = object : KontaktinfoClient {
        override suspend fun hentKontaktinfo(orgnr: String) =kontaktinfo.getOrDefault(orgnr, ingenKontaktinfo)
    }

    val eregClientMock = object : EregClient {
        override suspend fun hentOrganisasjon(orgnummer: String) =
            when (orgnummer) {
                underenhetOrgnr -> underenhet

                else -> null
            }
    }


    @Test
    fun `bruker kofuvi for underenhet om den finnes`() = runTestApplicationWithDatabase(
        dependenciesCfg = {
            provide(VarslingStatusRepository::class)
            provide<KontaktinfoClient> {
                mockKontaktinfoClient(mapOf(
                    underenhetOrgnr to kontaktinfoMedEpost,
                    hovedenhetOrgnr to kontaktinfoMedTlf
                ))
            }
            provide<EregClient> { eregClientMock }
            provide(KontaktInfoPollerRepository::class)
            provide(KontaktInfoPollingService::class)
        },
    ) {
        resolve<KontaktInfoPollerRepository>().schedulePoll(
            listOf(underenhetOrgnr),
            Instant.now().minusSeconds(60).toString()
        )
        resolve<KontaktInfoPollingService>().pollAndPullKontaktInfo()

        resolve<Database>().nonTransactionalExecuteQuery(
            "select * from kontaktinfo_resultat where virksomhetsnummer = ?",
            { text(underenhetOrgnr) },
            { mapOf(
                "har_epost" to it.getBoolean("har_epost"),
                "har_tlf" to it.getBoolean("har_tlf"),
            ) }
        ).let {
            assertEquals(
                mapOf(
                    "har_epost" to true,
                    "har_tlf" to false,
                ),
                it.first()
            )
        }
    }

    @Test
    fun `henter kofuvi for hovedenhet om det mangler kofuvi på underenhet`() = runTestApplicationWithDatabase(
        dependenciesCfg = {
            provide(VarslingStatusRepository::class)
            provide<KontaktinfoClient> {
                mockKontaktinfoClient(mapOf(
                    underenhetOrgnr to ingenKontaktinfo,
                    hovedenhetOrgnr to kontaktinfoMedEpost
                ))
            }
            provide<EregClient> { eregClientMock }
            provide(KontaktInfoPollerRepository::class)
            provide(KontaktInfoPollingService::class)
        },
    ) {
        resolve<KontaktInfoPollerRepository>().schedulePoll(
            listOf(underenhetOrgnr),
            Instant.now().minusSeconds(60).toString()
        )
        resolve<KontaktInfoPollingService>().pollAndPullKontaktInfo()

        resolve<Database>().nonTransactionalExecuteQuery(
            "select * from kontaktinfo_resultat where virksomhetsnummer = ?",
            { text(underenhetOrgnr) },
            { mapOf(
                "har_epost" to it.getBoolean("har_epost"),
                "har_tlf" to it.getBoolean("har_tlf"),
            ) }
        ).let {
            assertEquals(
                mapOf(
                    "har_epost" to true,
                    "har_tlf" to false,
                ),
                it.first()
            )
        }
    }
}