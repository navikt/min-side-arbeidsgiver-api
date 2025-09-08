package no.nav.arbeidsgiver.min_side.services.lagredefilter

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.arbeidsgiver.min_side.Database
import no.nav.arbeidsgiver.min_side.Database.Companion.executeUpdate
import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder
import java.sql.ResultSet
import java.time.Instant

class LagredeFilterService(private val database: Database) {
    suspend fun getAll(lock: Boolean = false, authenticatedUserHolder: AuthenticatedUserHolder): List<LagretFilter> {
        return database.nonTransactionalExecuteQuery(
            "SELECT * FROM lagrede_filter where fnr = ? ${if (lock) "for update" else ""}", {
                text(authenticatedUserHolder.fnr)
            }, { rs ->
                fromDbTransform(rs)
            }
        )
    }

    private suspend fun get(filterId: String, lock: Boolean = false, authenticatedUserHolder: AuthenticatedUserHolder): LagretFilter? {
        return database.nonTransactionalExecuteQuery(
            "SELECT * FROM lagrede_filter where fnr = ? and filter_id = ? ${if (lock) "for update" else ""}",
            {
                text(authenticatedUserHolder.fnr)
                text(filterId)
            },
            { rs ->
                fromDbTransform(rs)
            })
            .firstOrNull()
    }

    suspend fun delete(filterId: String, authenticatedUserHolder: AuthenticatedUserHolder): LagretFilter? {
        return database.transactional {
            val existing = get(filterId, true, authenticatedUserHolder) ?: return@transactional null
            executeUpdate(
                "DELETE FROM lagrede_filter WHERE fnr = ? AND filter_id = ?", {
                    text(authenticatedUserHolder.fnr)
                    text(filterId)
                }
            )
            existing
        }
    }

    suspend fun put(filter: LagretFilter, authenticatedUserHolder: AuthenticatedUserHolder): LagretFilter {
        return database.transactional {
            val mapper = jacksonObjectMapper()
            val now = Instant.now()
            val existing = get(filter.filterId, true, authenticatedUserHolder)
            if (existing == null) {
                executeUpdate(
                    "INSERT INTO lagrede_filter (filter_id, fnr, navn, side, tekstsoek, virksomheter, sortering, sakstyper, oppgave_filter, opprettet_tidspunkt, sist_endret_tidspunkt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    {
                        text(filter.filterId)
                        text(authenticatedUserHolder.fnr)
                        text(filter.navn)
                        integer(filter.side)
                        text(filter.tekstsoek)
                        text(mapper.writeValueAsString(filter.virksomheter))
                        text(filter.sortering.toString())
                        text(mapper.writeValueAsString(filter.sakstyper))
                        text(mapper.writeValueAsString(filter.oppgaveFilter))
                        timestamp_without_timezone_utc(now) // Opprettet tidspunkt
                        timestamp_without_timezone_utc(now) // Sist endret tidspunkt
                    }
                )
            } else {
                executeUpdate(
                    "UPDATE lagrede_filter SET navn = ?, side = ?, tekstsoek = ?, virksomheter = ?, sortering = ?, sakstyper = ?, oppgave_filter = ?, sist_endret_tidspunkt = ? WHERE fnr = ? AND filter_id = ?",
                    {
                        text(filter.navn)
                        integer(filter.side)
                        text(filter.tekstsoek)
                        text(mapper.writeValueAsString(filter.virksomheter))
                        text(filter.sortering.toString())
                        text(mapper.writeValueAsString(filter.sakstyper))
                        text(mapper.writeValueAsString(filter.oppgaveFilter))
                        timestamp_without_timezone_utc(now) // Sist endret tidspunkt
                        text(authenticatedUserHolder.fnr)
                        text(filter.filterId)
                    }
                )
            }
            filter
        }
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

