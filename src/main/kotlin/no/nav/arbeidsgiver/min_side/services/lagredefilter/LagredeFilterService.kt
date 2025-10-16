package no.nav.arbeidsgiver.min_side.services.lagredefilter

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant

@Service
class LagredeFilterService(val jdbcTemplate: JdbcTemplate) {
    fun getAll(fnr: String, lock: Boolean = false): List<LagretFilter> {
        return jdbcTemplate.query(
            "SELECT * FROM lagrede_filter where fnr = ? ${if (lock) "for update" else ""}", { ps: PreparedStatement ->
                ps.setString(1, fnr)
            }, { rs, _ ->
                fromDbTransform(rs)
            })
    }

    private fun get(fnr: String, filterId: String, lock: Boolean = false): LagretFilter? {
        return jdbcTemplate.query(
            "SELECT * FROM lagrede_filter where fnr = ? and filter_id = ? ${if (lock) "for update" else ""}",
            { ps: PreparedStatement ->
                ps.setString(1, fnr)
                ps.setString(2, filterId)
            },
            { rs, _ ->
                fromDbTransform(rs)
            })
            .firstOrNull()
    }

    @Transactional
    fun delete(fnr: String, filterId: String): LagretFilter? {
        val existing = get(fnr, filterId, true) ?: return null
        jdbcTemplate.update(
            "DELETE FROM lagrede_filter WHERE fnr = ? AND filter_id = ?", { ps: PreparedStatement ->
                ps.setString(1, fnr)
                ps.setString(2, filterId)
            }
        )
        return existing
    }

    @Transactional
    fun put(fnr: String, filter: LagretFilter): LagretFilter? {
        val mapper = jacksonObjectMapper()
        val now = Instant.now()
        val existing = get(fnr, filter.filterId, true)
        if (existing == null) {
            jdbcTemplate.update(
                "INSERT INTO lagrede_filter (filter_id, fnr, navn, side, tekstsoek, virksomheter, sortering, sakstyper, oppgave_filter, opprettet_tidspunkt, sist_endret_tidspunkt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                { ps: PreparedStatement ->
                    ps.setString(1, filter.filterId)
                    ps.setString(2, fnr)
                    ps.setString(3, filter.navn)
                    ps.setInt(4, filter.side)
                    ps.setString(5, filter.tekstsoek)
                    ps.setString(6, mapper.writeValueAsString(filter.virksomheter))
                    ps.setString(7, filter.sortering.toString())
                    ps.setString(8, mapper.writeValueAsString(filter.sakstyper))
                    ps.setString(9, mapper.writeValueAsString(filter.oppgaveFilter))
                    ps.setTimestamp(10, Timestamp.from(now)) // Opprettet tidspunkt
                    ps.setTimestamp(11, Timestamp.from(now)) // Sist endret tidspunkt
                }
            )
        } else {
            jdbcTemplate.update(
                "UPDATE lagrede_filter SET navn = ?, side = ?, tekstsoek = ?, virksomheter = ?, sortering = ?, sakstyper = ?, oppgave_filter = ?, sist_endret_tidspunkt = ? WHERE fnr = ? AND filter_id = ?",
                { ps: PreparedStatement ->
                    ps.setString(1, filter.navn)
                    ps.setInt(2, filter.side)
                    ps.setString(3, filter.tekstsoek)
                    ps.setString(4, mapper.writeValueAsString(filter.virksomheter))
                    ps.setString(5, filter.sortering.toString())
                    ps.setString(6, mapper.writeValueAsString(filter.sakstyper))
                    ps.setString(7, mapper.writeValueAsString(filter.oppgaveFilter))
                    ps.setTimestamp(8, Timestamp.from(now)) // Sist endret tidspunkt
                    ps.setString(9, fnr)
                    ps.setString(10, filter.filterId)
                }
            )
        }
        return filter
    }

    private fun fromDbTransform(row: ResultSet): LagretFilter {
        val mapper = jacksonObjectMapper()
        return LagretFilter(
            filterId = row.getString("filter_id"),
            navn = row.getString("navn"),
            side = row.getInt("side"), //fjerne?
            tekstsoek = row.getString("tekstsoek"),
            virksomheter = mapper.readValue<List<String>>(row.getString("virksomheter")),
            sortering = SakSortering.valueOf(row.getString("sortering")),
            sakstyper = mapper.readValue<List<String>>(row.getString("sakstyper")),
            oppgaveFilter = mapper.readValue<List<String>>(row.getString("oppgave_filter")),
        )
    }

    data class LagretFilter(
        val filterId: String,
        val navn: String,
        val side: Int, //fjerne denne?
        val tekstsoek: String,
        val virksomheter: List<String>,
        val sortering: SakSortering,
        val sakstyper: List<String>,
        val oppgaveFilter: List<String>
    )

    enum class SakSortering {
        NYESTE,
        ELDSTE,
    }
}