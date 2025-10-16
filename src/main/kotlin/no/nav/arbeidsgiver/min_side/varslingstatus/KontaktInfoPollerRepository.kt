package no.nav.arbeidsgiver.min_side.varslingstatus

import no.nav.arbeidsgiver.min_side.Database
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class KontaktInfoPollerRepository(
    private val database: Database,
) {

    suspend fun schedulePoll(virksomheterMedFeil: List<String>, pollTidspunkt: String) {
        database.batchUpdate(
            """
                insert into poll_kontaktinfo (virksomhetsnummer, poll_tidspunkt) values (?, ?)
                    on conflict (virksomhetsnummer) do update set poll_tidspunkt = EXCLUDED.poll_tidspunkt;
            """,
            virksomheterMedFeil.map { arrayOf(it, pollTidspunkt) }
        )

    }

    suspend fun updateKontaktInfo(
        virksomhetsnummer: String,
        harEpost: Boolean,
        harTlf: Boolean
    ) {
        database.nonTransactionalExecuteUpdate(
            """
                    insert into kontaktinfo_resultat (virksomhetsnummer, sjekket_tidspunkt, har_epost, har_tlf) 
                    values (?, ?, ?, ?)
                        on conflict (virksomhetsnummer) do update 
                            set sjekket_tidspunkt = EXCLUDED.sjekket_tidspunkt,
                                har_epost = EXCLUDED.har_epost,
                                har_tlf = EXCLUDED.har_tlf;
                """,
            {
                text(virksomhetsnummer)
                text(Instant.now().toString())
                boolean(harEpost)
                boolean(harTlf)
            }
        )
    }

    suspend fun getAndDeleteForPoll(): String? {
        val virksomhetsnummer = database.nonTransactionalExecuteQuery(
            """
                select virksomhetsnummer
                    from poll_kontaktinfo
                    where poll_tidspunkt < ?
                    order by poll_tidspunkt
                    limit 1
                    for update skip locked;
            """,
            {
                text(Instant.now().toString())
            },
            {
                it.getString("virksomhetsnummer")
            }
        ).firstOrNull()

        if (virksomhetsnummer == null) {
            return null
        }
        database.nonTransactionalExecuteUpdate(
            """
                delete from poll_kontaktinfo where virksomhetsnummer = ?;
            """,
            {
                text(virksomhetsnummer)
            }
        )
        return virksomhetsnummer
    }

    suspend fun slettKontaktinfoMedOkStatusEllerEldreEnn(retention: Duration) {
        database.nonTransactionalExecuteUpdate(
            """
                with newest_statuses as (
                    select virksomhetsnummer, max(status_tidspunkt) as newest_status_timestamp
                    from varsling_status
                    group by virksomhetsnummer
                ), ok_eller_utgått as (
                    select vs.virksomhetsnummer, vs.status_tidspunkt, vs.status
                            from varsling_status vs
                            join newest_statuses
                                on vs.virksomhetsnummer = newest_statuses.virksomhetsnummer 
                                and vs.status_tidspunkt = newest_statuses.newest_status_timestamp
                )
                delete from kontaktinfo_resultat
                    where virksomhetsnummer in (
                        select virksomhetsnummer from ok_eller_utgått
                        where status_tidspunkt < ?
                            or status = 'OK'
                    );
            """,
            {
                text(Instant.now().minus(retention.toJavaDuration()).toString())
            }
        )
    }
}