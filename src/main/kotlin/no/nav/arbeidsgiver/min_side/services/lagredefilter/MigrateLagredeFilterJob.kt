package no.nav.arbeidsgiver.min_side.services.lagredefilter

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.arbeidsgiver.min_side.services.storage.StorageEntry
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class MigrateLagredeFilterJob(
    val jdbcTemplate: JdbcTemplate,
) {
    val batchSize = 100

    @EventListener(ApplicationReadyEvent::class)
    fun run() {
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

    private fun deserializeStorageEntry(storageEntry: StorageEntry): List<LagretFilter> {
        val mapper = strictObjectMapper()
        val storageFilterListe = mapper.readValue<List<String>>(storageEntry.value)
        val lagredeFiltere = storageFilterListe.mapNotNull {
            var storageValue: StorageValue<out StorageFilterBase>
            try {
                storageValue = mapper.readValue<StorageValue<StorageFilterMedOppgaveFilter>>(it)
            } catch (ex: JsonProcessingException) {
                try {
                    storageValue = mapper.readValue<StorageValue<StorageFilterMedOppgaveTilstand>>(it)
                } catch (ex: JsonProcessingException) {
                    try {
                        storageValue = mapper.readValue<StorageValue<StorageFilterBase>>(it)
                    } catch (ex: JsonProcessingException) {
                        return@mapNotNull null
                    }
                }
            }
            LagretFilter(
                filterId = storageValue.uuid,
                fnr = storageEntry.fnr,
                navn = storageValue.navn,
                side = storageValue.filter.side,
                tekstsoek = storageValue.filter.tekstsoek,
                virksomheter = storageValue.filter.virksomheter ?: emptyList(),
                sortering = fiksSortering(storageValue.filter.sortering),
                sakstyper = storageValue.filter.sakstyper ?: emptyList(),
                oppgaveFilter = when (storageValue.filter) {
                    is StorageFilterMedOppgaveFilter -> (storageValue.filter as StorageFilterMedOppgaveFilter).oppgaveFilter ?: listOf()
                    is StorageFilterMedOppgaveTilstand -> konverterTilOppgaveFilter((storageValue.filter as StorageFilterMedOppgaveTilstand).oppgaveTilstand)
                    else -> listOf()
                },
                opprettetTidspunkt = storageEntry.timestamp,
                sistEndretTidspunkt = storageEntry.timestamp
            )
        }
        return lagredeFiltere
    }

    private fun fiksSortering(sortering: String?): String {
        if (sortering == null || sortering !in listOf("NYESTE", "ELDESTE")) {
            return "NYESTE"
        }
        return sortering
    }
}

private fun strictObjectMapper(): ObjectMapper {
    return jacksonObjectMapper().registerKotlinModule()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
        .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
        .configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, true)
        .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, true)
}

private data class StorageValue<T : StorageFilterBase>(
    val uuid: String,
    val navn: String,
    val filter: T,
)

private open class StorageFilterBase(
    val side: Int,
    val tekstsoek: String?,
    val virksomheter: List<String>?,
    val sortering: String?,
    val sakstyper: List<String>?
)

private class StorageFilterMedOppgaveFilter(
    side: Int,
    tekstsoek: String?,
    virksomheter: List<String>?,
    sortering: String?,
    sakstyper: List<String>?,
    val oppgaveFilter: List<String>?
) : StorageFilterBase(
    side, tekstsoek, virksomheter, sortering, sakstyper
)

private class StorageFilterMedOppgaveTilstand(
    side: Int,
    tekstsoek: String?,
    virksomheter: List<String>?,
    sortering: String?,
    sakstyper: List<String>?,
    val oppgaveTilstand: List<String>?
) : StorageFilterBase(
    side, tekstsoek, virksomheter, sortering, sakstyper
)


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
