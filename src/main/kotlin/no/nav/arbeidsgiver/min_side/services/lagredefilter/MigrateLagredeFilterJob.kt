package no.nav.arbeidsgiver.min_side.services.lagredefilter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.arbeidsgiver.min_side.config.logger
import no.nav.arbeidsgiver.min_side.services.storage.StorageEntry
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

//TODO: fjerne denne etter at migrering i prod er gjort
@Component
@Profile(
//    "prod-gcp",
    "dev-gcp"
)
class MigrateLagredeFilterJob(
    val jdbcTemplate: JdbcTemplate,
) {
    private val log = logger()

    @EventListener(ApplicationReadyEvent::class)
    @Transactional
    fun run() {
        try {
            log.info("Stareter migrering til lagrede_filter fra storage")
            // slett rader i lagrede_filter tabellen hvis den finnes
            jdbcTemplate.update(
                "DELETE FROM lagrede_filter where 1=1"
            )

            val storageEntries = jdbcTemplate.query(
                "select * from storage where key = 'lagrede-filter' and value is not null"
            ) { rs, _ ->
                StorageEntry(
                    key = rs.getString("key"),
                    fnr = rs.getString("fnr"),
                    value = rs.getString("value"),
                    version = rs.getInt("version"),
                    timestamp = rs.getString("timestamp")
                )
            }
            val lagredeFiltere = storageEntries.flatMap { deserializeStorageEntry(it) }
            val mapper = jacksonObjectMapper()
            if (lagredeFiltere.isNotEmpty()) {
                jdbcTemplate.batchUpdate(
                    "INSERT INTO lagrede_filter (filter_id, fnr, navn, side, tekstsoek, virksomheter, sortering, sakstyper, oppgave_filter, opprettet_tidspunkt, sist_endret_tidspunkt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    lagredeFiltere.map { filter ->
                        arrayOf(
                            filter.filterId,
                            filter.fnr,
                            filter.navn,
                            filter.side,
                            filter.tekstsoek,
                            mapper.writeValueAsString(filter.virksomheter),
                            filter.sortering,
                            mapper.writeValueAsString(filter.sakstyper),
                            mapper.writeValueAsString(filter.oppgaveFilter),
                            Timestamp.from(parseInstant(filter.opprettetTidspunkt)),
                            Timestamp.from(parseInstant(filter.sistEndretTidspunkt))
                        )
                    }
                )
            }
            log.info("Migrering til lagrede_filter fra storage fullført. ${lagredeFiltere.size} filtere migrert.")
        }
        catch (e: Exception) {
            log.error("Feil under migrering til lagrede_filter: ${e.message}", e)
            throw e // Rerthrow exception to ensure transaction rollback
        }
    }
}

private fun parseInstant(timestamp: String): Instant {
    val formatter = DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
        .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)
        .appendOffset("+HH:mm", "Z") // handles +00:00 style offset
        .toFormatter()

    return OffsetDateTime.parse(timestamp, formatter).toInstant()
}


private fun konverterTilOppgaveFilter(oppgaveTilstand: List<String>?): List<String> {
    return oppgaveTilstand?.mapNotNull { tilstand ->
        when (tilstand) {
            "Ny" -> "TILSTAND_NY"
            "Utfoert" -> "TILSTAND_UTFOERT"
            "Utgaatt" -> "TILSTAND_UTGAATT"
            else -> null
        }
    } ?: emptyList()
}

private fun fiksOppgaveFilter(filter: JsonNode): List<String> {
    if (filter.has("oppgaveFilter"))
        return filter.get("oppgaveFilter")?.map { it.asText() } ?: emptyList()
    if (filter.has("oppgaveTilstand")) {
        return konverterTilOppgaveFilter(filter.get("oppgaveTilstand").map { it.asText() })
    } else
        return emptyList()
}


private fun deserializeStorageEntry(storageEntry: StorageEntry): List<LagretFilter> {
    val mapper = jacksonObjectMapper()
    val storageFilterListe = mapper.readValue<List<JsonNode>>(storageEntry.value)
    val lagredeFiltere = storageFilterListe.mapNotNull { storageValue ->
        val filter = storageValue.get("filter")
        if (filter == null) {
            return@mapNotNull null // Hopp over hvis filter ikke finnes
        }

        LagretFilter(
            filterId = storageValue.get("uuid").asText(),
            fnr = storageEntry.fnr,
            navn = storageValue.get("navn").asText(),
            side = filter.get("side").asInt(),
            tekstsoek = filter.get("tekstsoek").asText(),
            virksomheter = filter.get("virksomheter")?.map { it.asText() } ?: emptyList(),
            sortering = fiksSortering(filter.get("sortering").asText()),
            sakstyper = filter.get("sakstyper")?.map { it.asText() } ?: emptyList(),
            oppgaveFilter = fiksOppgaveFilter(filter),
            opprettetTidspunkt = storageEntry.timestamp,
            sistEndretTidspunkt = storageEntry.timestamp
        )
    }
    return lagredeFiltere
}

private fun fiksSortering(sortering: String?): String {
    if (sortering == null || sortering !in listOf("NYESTE", "ELDSTE")) {
        return "NYESTE"
    }
    return sortering
}

private data class LagretFilter(
    val filterId: String,
    val fnr: String,
    val navn: String,
    val side: Int,
    val tekstsoek: String?,
    val virksomheter: List<String>,
    val sortering: String?,
    val sakstyper: List<String>,
    val oppgaveFilter: List<String>,
    val opprettetTidspunkt: String,
    val sistEndretTidspunkt: String
)
