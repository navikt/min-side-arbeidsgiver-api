package no.nav.arbeidsgiver.min_side.varslingstatus

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration

@MockitoBean(types=[JwtDecoder::class])
@SpringBootTest(
    properties = [
        "spring.flyway.cleanDisabled=false",
    ]
)
class KontaktInfoPollerRepositoryTest {

    @Autowired
    lateinit var varslingStatusRepository: VarslingStatusRepository

    @Autowired
    lateinit var kontaktInfoPollerRepository: KontaktInfoPollerRepository

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

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
    fun `sletter kontaktinfo med ok status eller eldre enn`() {
        listOf(
            // OK
            Triple("OK", "42", Instant.now().toString()),

            // OK and old
            Triple("OK", "43", Instant.now().minus(2.days.toJavaDuration()).toString()),

            // MANGLER_KOFUVI
            Triple("MANGLER_KOFUVI", "314", Instant.now().toString()),

            // MANGLER_KOFUVI and old
            Triple("MANGLER_KOFUVI", "315", Instant.now().minus(2.days.toJavaDuration()).toString()),
        ).forEachIndexed { index, (status, vnr, tidspunkt) ->
            processVarslingStatus(
                """
                {
                        "virksomhetsnummer": "$vnr",
                        "varselId": "vid$index",
                        "varselTimestamp": "2021-01-01T00:00:00",
                        "kvittertEventTimestamp": "$tidspunkt",
                        "status": "$status",
                        "version": "1"
                    }
                """.trimIndent()
            )
            kontaktInfoPollerRepository.updateKontaktInfo(vnr, harEpost = true, harTlf = true)
        }

        kontaktInfoPollerRepository.slettKontaktinfoMedOkStatusEllerEldreEnn(1.days)

        val kontaktinfo = jdbcTemplate.queryForList(
            """
           select * from kontaktinfo_resultat 
        """
        ).map { it["virksomhetsnummer"] as String }

        assertEquals(listOf("314"), kontaktinfo)
    }


    private fun processVarslingStatus(value: String) {
        varslingStatusKafkaListener.processVarslingStatus(
            ConsumerRecord(
                "", 0, 0, "", value
            )
        )
    }
}