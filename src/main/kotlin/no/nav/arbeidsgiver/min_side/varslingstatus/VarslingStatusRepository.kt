package no.nav.arbeidsgiver.min_side.varslingstatus

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.arbeidsgiver.min_side.varslingstatus.VarslingStatusDto.Status
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.sql.PreparedStatement
import java.time.Instant
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlin.time.toJavaDuration

data class VarslingStatus(
    val status: Status,
    val varselTimestamp: LocalDateTime,
    val kvittertEventTimestamp: Instant,
)

@Repository
class VarslingStatusRepository(
    private val jdbcTemplate: JdbcTemplate,
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {
    fun varslingStatus(virksomhetsnummer: String): VarslingStatus {
        return namedParameterJdbcTemplate.queryForList(
            """
            select status, varslet_tidspunkt, status_tidspunkt
                from varsling_status vs
                left join kontaktinfo_resultat ki using (virksomhetsnummer)
                where virksomhetsnummer = :virksomhetsnummer
                    and (ki is null or (ki.har_epost = false and ki.har_tlf = false))
                order by status_tidspunkt desc
            """.trimIndent(),
            mapOf("virksomhetsnummer" to virksomhetsnummer)
        ).firstOrNull()?.let {
            VarslingStatus(
                status = Status.valueOf(it["status"] as String),
                varselTimestamp = LocalDateTime.parse(it["varslet_tidspunkt"] as String),
                kvittertEventTimestamp = Instant.parse(it["status_tidspunkt"] as String),
            )
        } ?: VarslingStatus(
            status = Status.OK,
            varselTimestamp = LocalDateTime.now(),
            kvittertEventTimestamp = Instant.now(),
        )
    }

    fun hentVirksomheterMedFeil(maxAge: Duration): List<String> {
        return jdbcTemplate.queryForList(
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
            Instant.now().minus(maxAge.toJavaDuration()).toString()
        ).map { it["virksomhetsnummer"] as String }
    }

    fun processVarslingStatus(varslingStatus: VarslingStatusDto) {
        jdbcTemplate.update(
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
        ) { ps: PreparedStatement ->
            ps.setString(1, varslingStatus.varselId)
            ps.setString(2, varslingStatus.virksomhetsnummer)
            ps.setString(3, varslingStatus.status.toString())
            ps.setString(4, varslingStatus.eventTimestamp.toString())
            ps.setString(5, varslingStatus.varselTimestamp.toString())
        }
    }

    fun slettVarslingStatuserEldreEnn(retention: Duration) {
        jdbcTemplate.update(
            """
            delete from varsling_status where varsling_status.status_tidspunkt < ?
            """,
            Instant.now().minus(retention.toJavaDuration()).toString()
        )
    }
}

@Profile("dev-gcp", "prod-gcp")
@Service
class VarslingStatusKafkaListener(
    private val varslingStatusRepository: VarslingStatusRepository,
    private val objectMapper: ObjectMapper,
) {
    @Profile("dev-gcp", "prod-gcp")
    @KafkaListener(
        id = "min-side-arbeidsgiver-varsling-status-1",
        topics = ["fager.ekstern-varsling-status"],
        containerFactory = "errorLoggingKafkaListenerContainerFactory"
    )
    fun processVarslingStatus(record: ConsumerRecord<String?, String?>) =
        varslingStatusRepository.processVarslingStatus(
            objectMapper.readValue(record.value(), VarslingStatusDto::class.java)
        )
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


