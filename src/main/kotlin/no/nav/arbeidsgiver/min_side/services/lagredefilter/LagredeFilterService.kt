package no.nav.arbeidsgiver.min_side.services.lagredefilter

import kotlinx.serialization.Serializable
import no.nav.arbeidsgiver.min_side.infrastruktur.Database
import no.nav.arbeidsgiver.min_side.infrastruktur.Database.Companion.executeUpdate
import no.nav.arbeidsgiver.min_side.infrastruktur.defaultJson
import java.sql.ResultSet
import java.time.Instant

class LagredeFilterService(private val database: Database) {
    suspend fun getAll(lock: Boolean = false, fnr: String): List<LagretFilter> {
        return database.nonTransactionalExecuteQuery(
            "SELECT * FROM lagrede_filter where fnr = ? ${if (lock) "for update" else ""}", {
                text(fnr)
            }, { rs ->
                fromDbTransform(rs)
            }
        )
    }

    private suspend fun get(filterId: String, lock: Boolean = false, fnr: String): LagretFilter? {
        return database.nonTransactionalExecuteQuery(
            "SELECT * FROM lagrede_filter where fnr = ? and filter_id = ? ${if (lock) "for update" else ""}",
            {
                text(fnr)
                text(filterId)
            },
            { rs ->
                fromDbTransform(rs)
            })
            .firstOrNull()
    }

    suspend fun delete(filterId: String, fnr: String): LagretFilter? {
        return database.transactional {
            val existing = get(filterId, true, fnr) ?: return@transactional null
            executeUpdate(
                "DELETE FROM lagrede_filter WHERE fnr = ? AND filter_id = ?"
            ) {
                text(fnr)
                text(filterId)
            }
            existing
        }
    }

    suspend fun put(filter: LagretFilter, fnr: String): LagretFilter {
        return database.transactional {
            val now = Instant.now()
            val existing = get(filter.filterId, true, fnr)
            if (existing == null) {
                executeUpdate(
                    """
                    INSERT INTO lagrede_filter (filter_id, fnr, navn, side, tekstsoek, virksomheter, sortering, sakstyper, oppgave_filter, opprettet_tidspunkt, sist_endret_tidspunkt)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    {
                        text(filter.filterId)
                        text(fnr)
                        text(filter.navn)
                        integer(filter.side)
                        text(filter.tekstsoek)
                        text(defaultJson.encodeToString(filter.virksomheter))
                        text(filter.sortering.toString())
                        text(defaultJson.encodeToString(filter.sakstyper))
                        text(defaultJson.encodeToString(filter.oppgaveFilter))
                        timestamp_without_timezone_utc(now) // Opprettet tidspunkt
                        timestamp_without_timezone_utc(now) // Sist endret tidspunkt
                    }
                )
            } else {
                executeUpdate(
                    """
                    UPDATE lagrede_filter
                    SET navn                  = ?,
                        side                  = ?,
                        tekstsoek             = ?,
                        virksomheter          = ?,
                        sortering             = ?,
                        sakstyper             = ?,
                        oppgave_filter        = ?,
                        sist_endret_tidspunkt = ?
                    WHERE fnr = ?
                      AND filter_id = ?    
                    """.trimIndent(),
                    {
                        text(filter.navn)
                        integer(filter.side)
                        text(filter.tekstsoek)
                        text(defaultJson.encodeToString(filter.virksomheter))
                        text(filter.sortering.toString())
                        text(defaultJson.encodeToString(filter.sakstyper))
                        text(defaultJson.encodeToString(filter.oppgaveFilter))
                        timestamp_without_timezone_utc(now) // Sist endret tidspunkt
                        text(fnr)
                        text(filter.filterId)
                    }
                )
            }
            filter
        }
    }

    private fun fromDbTransform(row: ResultSet): LagretFilter {
        return LagretFilter(
            filterId = row.getString("filter_id"),
            navn = row.getString("navn"),
            side = row.getInt("side"), //fjerne?
            tekstsoek = row.getString("tekstsoek"),
            virksomheter = defaultJson.decodeFromString<List<String>>(row.getString("virksomheter")),
            sortering = SakSortering.valueOf(row.getString("sortering")),
            sakstyper = defaultJson.decodeFromString<List<String>>(row.getString("sakstyper")),
            oppgaveFilter = defaultJson.decodeFromString<List<String>>(row.getString("oppgave_filter")),
        )
    }

    @Serializable
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

