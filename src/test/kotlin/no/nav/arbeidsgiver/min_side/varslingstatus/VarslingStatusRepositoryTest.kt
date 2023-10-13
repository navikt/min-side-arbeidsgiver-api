package no.nav.arbeidsgiver.min_side.varslingstatus

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    properties = [
        "server.servlet.context-path=/",
        "spring.flyway.cleanDisabled=false",
    ]
)
class VarslingStatusRepositoryTest {

    @Autowired
    lateinit var varslingStatusRepository: VarslingStatusRepository

    @Autowired
    lateinit var objectMapper: ObjectMapper

    lateinit var varslingStatusKafkaListener: VarslingStatusKafkaListener

    @Autowired
    lateinit var flyway: Flyway

    @BeforeEach
    fun setup() {
        flyway.clean()
        flyway.migrate()
        varslingStatusKafkaListener = VarslingStatusKafkaListener(
            varslingStatusRepository,
            objectMapper,
        )
    }

    @Test
    fun `henter virksomheter som har MANGLER_KOFUVI som nyeste status`() {
        listOf(
            // 42 siste status = OK
            Triple("OK", "2021-01-03T00:00:00Z", "42"),
            Triple("MANGLER_KOFUVI", "2021-01-02T00:00:00Z", "42"),

            // 314 siste status = MANGLER_KOFUVI
            Triple("MANGLER_KOFUVI", "2021-01-03T00:00:00Z", "314"),
            Triple("OK", "2021-01-02T00:00:00Z", "314"),
        ).forEachIndexed { index, (status, timestamp, vnr) ->
            processVarslingStatus(
                """
                    {
                        "virksomhetsnummer": "314",
                        "varselId": "vid$index",
                        "varselTimestamp": "2021-01-01T00:00:00",
                        "kvittertEventTimestamp": "$timestamp",
                        "status": "$status",
                        "version": "1"
                    }
                """
            )
        }

        val result = varslingStatusRepository.hentVirksomheterMedFeil()
        assertEquals(listOf("314"), result)
    }


    private fun processVarslingStatus(value: String) {
        varslingStatusKafkaListener.processVarslingStatus(
            ConsumerRecord(
                "", 0, 0, "", value
            )
        )
    }
}