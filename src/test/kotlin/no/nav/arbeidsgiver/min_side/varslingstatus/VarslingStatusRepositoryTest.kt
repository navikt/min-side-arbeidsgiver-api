//package no.nav.arbeidsgiver.min_side.varslingstatus
//
//import com.fasterxml.jackson.databind.ObjectMapper
//import org.apache.kafka.clients.consumer.ConsumerRecord
//import org.flywaydb.core.Flyway
//import org.junit.jupiter.api.Assertions.assertEquals
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.Test
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.boot.test.context.SpringBootTest
//import org.springframework.security.oauth2.jwt.JwtDecoder
//import org.springframework.test.context.bean.override.mockito.MockitoBean
//import java.time.Instant
//import kotlin.time.Duration.Companion.days
//import kotlin.time.Duration.Companion.seconds
//
//@MockitoBean(types=[JwtDecoder::class])
//@SpringBootTest(
//    properties = [
//        "spring.flyway.cleanDisabled=false",
//    ]
//)
//class VarslingStatusRepositoryTest {
//
//    @Autowired
//    lateinit var varslingStatusRepository: VarslingStatusRepository
//
//    @Autowired
//    lateinit var kontaktInfoPollerRepository: KontaktInfoPollerRepository
//
//    @Autowired
//    lateinit var objectMapper: ObjectMapper
//
//    lateinit var varslingStatusKafkaListener: VarslingStatusKafkaListener
//
//    @Autowired
//    lateinit var flyway: Flyway
//
//    @BeforeEach
//    fun setup() {
//        flyway.clean()
//        flyway.migrate()
//        varslingStatusKafkaListener = VarslingStatusKafkaListener(
//            varslingStatusRepository,
//            objectMapper,
//        )
//    }
//
//    @Test
//    fun `henter virksomheter som har MANGLER_KOFUVI som nyeste status`() {
//        listOf(
//            // 42 siste status = OK
//            Triple("OK", "2021-01-03T00:00:00Z", "42"),
//            Triple("MANGLER_KOFUVI", "2021-01-02T00:00:00Z", "42"),
//
//            // 314 siste status = MANGLER_KOFUVI
//            Triple("MANGLER_KOFUVI", "2021-01-03T00:00:00Z", "314"),
//            Triple("OK", "2021-01-02T00:00:00Z", "314"),
//
//            // 333 siste status = MANGLER_KOFUVI, men har kontaktinfo fra poll
//            Triple("MANGLER_KOFUVI", "2021-01-03T00:00:00Z", "333"),
//            Triple("OK", "2021-01-02T00:00:00Z", "333"),
//        ).forEachIndexed { index, (status, timestamp, vnr) ->
//            processVarslingStatus(
//                """
//                    {
//                        "virksomhetsnummer": "$vnr",
//                        "varselId": "vid$index",
//                        "varselTimestamp": "2021-01-01T00:00:00",
//                        "kvittertEventTimestamp": "$timestamp",
//                        "status": "$status",
//                        "version": "1"
//                    }
//                """
//            )
//        }
//        kontaktInfoPollerRepository.updateKontaktInfo("333", true, true)
//
//        val result = varslingStatusRepository.hentVirksomheterMedFeil(10000.days)
//        assertEquals(listOf("314"), result)
//    }
//
//    @Test
//    fun `sletter statuser eldre enn`() {
//        val now = Instant.now()
//        listOf(
//            now.minusSeconds(1) to "1000",
//            now.minusSeconds(2) to "2000",
//            now.minusSeconds(3) to "3000",
//            now.minusSeconds(4) to "4000",
//            now.minusSeconds(5) to "5000",
//            now.minusSeconds(6) to "6000",
//        ).forEachIndexed { index, (timestamp, vnr) ->
//            processVarslingStatus(
//                """
//                    {
//                        "virksomhetsnummer": "$vnr",
//                        "varselId": "vid$index",
//                        "varselTimestamp": "2021-01-01T00:00:00",
//                        "kvittertEventTimestamp": "$timestamp",
//                        "status": "MANGLER_KOFUVI",
//                        "version": "1"
//                    }
//                """
//            )
//        }
//        varslingStatusRepository.slettVarslingStatuserEldreEnn(3.seconds)
//
//        val result = varslingStatusRepository.hentVirksomheterMedFeil(1.days)
//        assertEquals(listOf("1000", "2000").sorted(), result.sorted())
//    }
//
//
//    private fun processVarslingStatus(value: String) {
//        varslingStatusKafkaListener.processVarslingStatus(
//            ConsumerRecord(
//                "", 0, 0, "", value
//            )
//        )
//    }
//}