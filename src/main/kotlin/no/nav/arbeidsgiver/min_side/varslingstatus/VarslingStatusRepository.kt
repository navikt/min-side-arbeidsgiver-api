package no.nav.arbeidsgiver.min_side.varslingstatus

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.sql.PreparedStatement
import java.time.LocalDateTime

data class VarslingStatus(
    val status: Status,
    val varselTimestamp: LocalDateTime,
    val eventTimestamp: LocalDateTime,
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
                from varsling_status
                where virksomhetsnummer = :virksomhetsnummer
                order by status_tidspunkt desc
            """.trimIndent(),
            mapOf("virksomhetsnummer" to virksomhetsnummer)
        ).firstOrNull()?.let {
            VarslingStatus(
                status = Status.valueOf(it["status"] as String),
                varselTimestamp = LocalDateTime.parse(it["varslet_tidspunkt"] as String),
                eventTimestamp = LocalDateTime.parse(it["status_tidspunkt"] as String),
            )
        } ?: VarslingStatus(
            status = Status.OK,
            varselTimestamp = LocalDateTime.now(),
            eventTimestamp = LocalDateTime.now(),
        )
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
    @param:JsonProperty("eventTimestamp") val eventTimestamp: LocalDateTime,
    @param:JsonProperty("status") val status: Status,
    @param:JsonProperty("version") val version: String,
)

enum class Status {
    OK,
    MANGLER_KOFUVI,
    ANNEN_FEIL,
}


