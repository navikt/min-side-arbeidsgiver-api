package no.nav.arbeidsgiver.min_side.services.tiltak

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
import java.sql.PreparedStatement

interface RefusjonStatusRepository {
    data class Statusoversikt(
        val virksomhetsnummer: String = "",
        /**
         * Antall refusjoner per status. Kan være tomt map. Hvis det ikke er noen saker med gitt status,
         * så er ikke nødvendigvis statusen i mappet.
         */
        val statusoversikt: Map<String, Int> = mapOf()
    )

    fun statusoversikt(virksomhetsnummer: List<String>): List<Statusoversikt>
}

@Profile("dev-gcp", "prod-gcp")
@Repository
class RefusjonStatusRepositoryImpl(
    private val objectMapper: ObjectMapper,
    private val jdbcTemplate: JdbcTemplate,
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) : RefusjonStatusRepository {

    @Profile("dev-gcp", "prod-gcp")
    @KafkaListener(
        id = "min-side-arbeidsgiver-1",
        topics = ["arbeidsgiver.tiltak-refusjon-endret-status"],
        containerFactory = "errorLoggingKafkaListenerContainerFactory"
    )
    fun processConsumerRecord(record: ConsumerRecord<String?, String?>) {
        val hendelse = objectMapper.readValue(record.value(), RefusjonStatusHendelse::class.java)
        jdbcTemplate.update(
            """
            insert into refusjon_status(refusjon_id, virksomhetsnummer, avtale_id, status) 
                values(?, ?, ?, ?) 
            on conflict (refusjon_id) 
                do update set 
                    virksomhetsnummer = EXCLUDED.virksomhetsnummer,        
                    avtale_id = EXCLUDED.avtale_id,        
                    status = EXCLUDED.status;
            """.trimIndent()
        ) { ps: PreparedStatement ->
            ps.setString(1, hendelse.refusjonId)
            ps.setString(2, hendelse.virksomhetsnummer)
            ps.setString(3, hendelse.avtaleId)
            ps.setString(4, hendelse.status)
        }
    }

    override fun statusoversikt(virksomhetsnummer: List<String>): List<RefusjonStatusRepository.Statusoversikt> {
        if (virksomhetsnummer.isEmpty()) {
            return listOf()
        }
        val grouped = namedParameterJdbcTemplate.queryForList(
            """
            select virksomhetsnummer, status, count(*) as count 
                from refusjon_status 
                where virksomhetsnummer in (:virksomhetsnumre) 
                group by virksomhetsnummer, status
            """.trimIndent(),
            mapOf("virksomhetsnumre" to virksomhetsnummer)
        )
            .groupBy { it["virksomhetsnummer"] }
            .mapValues {
                it.value
                    .associateBy { v -> v["status"] as String }
                    .mapValues { v -> (v.value["count"] as Long).toInt() }
            }
        return grouped.map { RefusjonStatusRepository.Statusoversikt(it.key as String, it.value) }
    }
}

@Profile("local")
@Repository
class RefusjonStatusRepositoryMock : RefusjonStatusRepository {
    override fun statusoversikt(virksomhetsnummer: List<String>): List<RefusjonStatusRepository.Statusoversikt> {
        return listOf()
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class RefusjonStatusHendelse @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) constructor(
    @param:JsonProperty("refusjonId") val refusjonId: String,
    @param:JsonProperty("bedriftNr") val virksomhetsnummer: String,
    @param:JsonProperty("avtaleId") val avtaleId: String,
    @param:JsonProperty("status") val status: String
)