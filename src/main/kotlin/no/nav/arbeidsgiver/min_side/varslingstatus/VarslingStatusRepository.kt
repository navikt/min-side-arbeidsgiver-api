package no.nav.arbeidsgiver.min_side.varslingstatus

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.arbeidsgiver.min_side.Database
import no.nav.arbeidsgiver.min_side.varslingstatus.VarslingStatusDto.Status
import java.time.Instant
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlin.time.toJavaDuration

data class VarslingStatus(
    val status: Status,
    val varselTimestamp: LocalDateTime,
    val kvittertEventTimestamp: Instant,
)

class VarslingStatusRepository(
    private val database: Database,
) {
    suspend fun varslingStatus(virksomhetsnummer: String): VarslingStatus {
        return database.nonTransactionalExecuteQuery(
            """
            select status, varslet_tidspunkt, status_tidspunkt
                from varsling_status vs
                left join kontaktinfo_resultat ki using (virksomhetsnummer)
                where virksomhetsnummer = ?
                    and (ki is null or (ki.har_epost = false and ki.har_tlf = false))
                order by status_tidspunkt desc
            """.trimIndent(),
            {
                text(virksomhetsnummer)
            },
            { rs ->
                VarslingStatus(
                    status = Status.valueOf(rs.getString("status")),
                    varselTimestamp = LocalDateTime.parse(rs.getString("varslet_tidspunkt")),
                    kvittertEventTimestamp = Instant.parse(rs.getString("status_tidspunkt"))
                )
            }).firstOrNull() ?:
            VarslingStatus(
            status = Status.OK,
            varselTimestamp = LocalDateTime.now(),
            kvittertEventTimestamp = Instant.now(),
        )
    }

    suspend fun hentVirksomheterMedFeil(maxAge: Duration): List<String> {
        return database.nonTransactionalExecuteQuery(
            """
            with newest_statuses as (
                select virksomhetsnummer, max(status_tidspunkt) as newest_status_timestamp
                from varsling_status
                group by virksomhetsnummer
            ) 
            select vs.*
            from varsling_status vs
            join newest_statuses
                on vs.virksomhetsnummer = newest_statuses.virksomhetsnummer 
                and vs.status_tidspunkt = newest_statuses.newest_status_timestamp
            left join kontaktinfo_resultat ki on vs.virksomhetsnummer = ki.virksomhetsnummer
                where (ki is null or (ki.har_epost = false and ki.har_tlf = false)) 
                and vs.status = 'MANGLER_KOFUVI'
                and newest_statuses.newest_status_timestamp > ?;
            """,
            {
                text(Instant.now().minus(maxAge.toJavaDuration()).toString())
            },
            { rs ->
                rs.getString("virksomhetsnummer")
            }
        )
    }


    suspend fun processVarslingStatus(varslingStatus: VarslingStatusDto) {
        database.nonTransactionalExecuteUpdate(
            """
            insert into varsling_status(
                varsel_id, virksomhetsnummer, status, status_tidspunkt, varslet_tidspunkt
            ) values(?, ?, ?, ?, ?) 
            on conflict (varsel_id) 
                -- upserter bare for sikkerhetsskyld, vil antakelig ikke skje
                do update set 
                    status = EXCLUDED.status,        
                    status_tidspunkt = EXCLUDED.status_tidspunkt,        
                    varslet_tidspunkt = EXCLUDED.varslet_tidspunkt;
            """.trimIndent()
        ) {
            text(varslingStatus.varselId)
            text(varslingStatus.virksomhetsnummer)
            text(varslingStatus.status.toString())
            text(varslingStatus.eventTimestamp.toString())
            text(varslingStatus.varselTimestamp.toString())
        }
    }

    suspend fun slettVarslingStatuserEldreEnn(retention: Duration) {
        database.nonTransactionalExecuteUpdate(
            """
            delete from varsling_status where varsling_status.status_tidspunkt < ?
            """,
            {
                text(Instant.now().minus(retention.toJavaDuration()).toString())
            }
        )
    }
}


@JsonIgnoreProperties(ignoreUnknown = true)
data class VarslingStatusDto @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) constructor(
    @param:JsonProperty("virksomhetsnummer") val virksomhetsnummer: String,
    @param:JsonProperty("varselId") val varselId: String,
    @param:JsonProperty("varselTimestamp") val varselTimestamp: LocalDateTime,
    @param:JsonProperty("kvittertEventTimestamp") val eventTimestamp: Instant,
    @param:JsonProperty("status") val status: Status,
    @param:JsonProperty("version") val version: String,
) {
    enum class Status {
        OK,
        MANGLER_KOFUVI,
        ANNEN_FEIL,
    }
}


