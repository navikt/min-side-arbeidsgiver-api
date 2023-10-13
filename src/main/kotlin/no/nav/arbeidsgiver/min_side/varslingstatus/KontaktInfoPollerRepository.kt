package no.nav.arbeidsgiver.min_side.varslingstatus

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class KontaktInfoPollerRepository(
    private val jdbcTemplate: JdbcTemplate,
) {

    fun schedulePoll(virksomheterMedFeil: List<String>, pollTidspunkt: String) {
        jdbcTemplate.batchUpdate("""
                insert into poll_kontaktinfo (virksomhetsnummer, poll_tidspunkt) values (?, ?)
                    on conflict (virksomhetsnummer) do update set poll_tidspunkt = EXCLUDED.poll_tidspunkt;
            """, virksomheterMedFeil.map { arrayOf(it, pollTidspunkt) })
    }

    fun updateKontaktInfo(
        virksomhetsnummer: String,
        harEpost: Boolean,
        harTlf: Boolean
    ) {
        jdbcTemplate.update(
            """
                    insert into kontaktinfo_resultat (virksomhetsnummer, sjekket_tidspunkt, har_epost, har_tlf) 
                    values (?, ?, ?, ?)
                        on conflict (virksomhetsnummer) do update 
                            set sjekket_tidspunkt = EXCLUDED.sjekket_tidspunkt,
                                har_epost = EXCLUDED.har_epost,
                                har_tlf = EXCLUDED.har_tlf;
                """,
            virksomhetsnummer,
            Instant.now().toString(),
            harEpost,
            harTlf
        )
    }

    fun getAndDeleteForPoll(): String? {
        val virksomhetsnummer = jdbcTemplate.queryForList(
            """
                select virksomhetsnummer
                    from poll_kontaktinfo
                    where poll_tidspunkt < ?
                    order by poll_tidspunkt
                    limit 1
                    for update skip locked;
            """,
            Instant.now().toString()
        ).firstOrNull()?.let { it["virksomhetsnummer"] as String } ?: return null

        jdbcTemplate.update(
            """
                delete from poll_kontaktinfo where virksomhetsnummer = ?;
            """,
            virksomhetsnummer
        )
        return virksomhetsnummer
    }
}