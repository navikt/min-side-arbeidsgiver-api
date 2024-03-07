package no.nav.arbeidsgiver.min_side.sykefraværstatistikk

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.arbeidsgiver.min_side.controller.SecurityMockMvcUtil.Companion.jwtWithPid
import no.nav.arbeidsgiver.min_side.models.Organisasjon
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.skyscreamer.jsonassert.JSONAssert.assertEquals
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(
    properties = [
        "server.servlet.context-path=/",
        "spring.flyway.cleanDisabled=false",
    ]
)
@AutoConfigureMockMvc
class SykefraværstatistikkIntegrationTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var sykefraværstatistikkRepository: SykefraværstatistikkRepository

    @Autowired
    lateinit var objectMapper: ObjectMapper

    lateinit var sykefraværstatistikkKafkaListener: SykefraværstatistikkKafkaListener

    @MockBean // the real jwt decoder is bypassed by SecurityMockMvcRequestPostProcessors.jwt
    lateinit var jwtDecoder: JwtDecoder

    @MockBean
    lateinit var altinnService: AltinnService

    @Autowired
    lateinit var flyway: Flyway

    @BeforeEach
    fun setup() {
        flyway.clean()
        flyway.migrate()
        sykefraværstatistikkKafkaListener = SykefraværstatistikkKafkaListener(
            sykefraværstatistikkRepository,
            objectMapper,
        )
    }

    @Test
    fun `bruker som representerer virksomhet med tilgang får virksomhetstatistikk`() {
        `when`(
            altinnService.hentOrganisasjonerBasertPaRettigheter("42", "3403", "1")
        ).thenReturn(listOf(Organisasjon(organizationNumber = "123", name = "Foo & Co")))
        processStatistikkkategori(
            """
                {
                    "kode": "123",
                    "kategori": "VIRKSOMHET",
                    "sistePubliserteKvartal": {
                        "prosent": 3.15,
                        "arstall": "2023",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 3.14,
                        "arstall": "2023",
                        "kvartal": "1"
                    }
                }
            """
        )
        processStatistikkkategori(
            """
                {
                    "kode": "123",
                    "kategori": "VIRKSOMHET",
                    "sistePubliserteKvartal": {
                        "prosent": 2.15,
                        "arstall": "2022",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 2.14,
                        "arstall": "2022",
                        "kvartal": "1"
                    }
                }
            """
        )


        mockMvc
            .perform(
                get("/api/sykefravaerstatistikk/{orgnr}", "123")
                    .with(jwtWithPid("42"))
            )
            .andExpect(status().isOk)
            .andReturn().response.contentAsString.also {
                assertEquals(
                    """
                    {
                        "type": "VIRKSOMHET",
                        "label": "Foo & Co",
                        "prosent": 3.14
                    }
                """, it, true
                )
            }
    }

    @Test
    fun `bruker uten tilgang får statistikk for bransje`() {
        `when`(
            altinnService.hentOrganisasjonerBasertPaRettigheter("42", "3403", "1")
        ).thenReturn(listOf(Organisasjon(organizationNumber = "321", name = "Coo & Fo")))
        processMetadataVirksomhet(
            """
                {
                    "orgnr": "123",
                    "bransje": "Testing",
                    "naring": "IT",
                    "arstall": "2023",
                    "kvartal": "1"
                }
            """
        )
        processMetadataVirksomhet(
            """
                {
                    "orgnr": "123",
                    "bransje": "Testing Gammel",
                    "naring": "IT Gammel",
                    "arstall": "2022",
                    "kvartal": "1"
                }
            """
        )
        processStatistikkkategori(
            """
                {
                    "kode": "Testing",
                    "kategori": "BRANSJE",
                    "sistePubliserteKvartal": {
                        "prosent": 3.15,
                        "arstall": "2023",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 3.14,
                        "arstall": "2023",
                        "kvartal": "1"
                    }
                }
            """
        )
        processStatistikkkategori(
            """
                {
                    "kode": "Testing Gammel",
                    "kategori": "BRANSJE",
                    "sistePubliserteKvartal": {
                        "prosent": 2.15,
                        "arstall": "2022",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 2.14,
                        "arstall": "2022",
                        "kvartal": "1"
                    }
                }
            """
        )
        processStatistikkkategori(
            """
                {
                    "kode": "IT",
                    "kategori": "NÆRING",
                    "sistePubliserteKvartal": {
                        "prosent": 3.16,
                        "arstall": "2023",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 3.17,
                        "arstall": "2023",
                        "kvartal": "1"
                    }
                }
            """
        )
        processStatistikkkategori(
            """
                {
                    "kode": "IT Gammel",
                    "kategori": "NÆRING",
                    "sistePubliserteKvartal": {
                        "prosent": 2.16,
                        "arstall": "2022",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 2.17,
                        "arstall": "2022",
                        "kvartal": "1"
                    }
                }
            """
        )


        mockMvc
            .perform(
                get("/api/sykefravaerstatistikk/{orgnr}", "123")
                    .with(jwtWithPid("42"))
            )
            .andExpect(status().isOk)
            .andReturn().response.contentAsString.also {
                assertEquals(
                    """
                    {
                        "type": "BRANSJE",
                        "label": "Testing",
                        "prosent": 3.14
                    }
                """, it, true
                )
            }
    }

    @Test
    fun `bruker uten tilgang får statistikk for næring`() {
        `when`(
            altinnService.hentOrganisasjonerBasertPaRettigheter("42", "3403", "1")
        ).thenReturn(listOf(Organisasjon(organizationNumber = "321", name = "Coo & Fo")))
        processMetadataVirksomhet(
            """
                {
                    "orgnr": "123",
                    "bransje": "Testing",
                    "naring": "IT",
                    "arstall": "2023",
                    "kvartal": "1"
                }
            """
        )
        processStatistikkkategori(
            """
                {
                    "kode": "IT",
                    "kategori": "NÆRING",
                    "sistePubliserteKvartal": {
                        "prosent": 3.15,
                        "arstall": "2023",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 3.14,
                        "arstall": "2023",
                        "kvartal": "1"
                    }
                }
            """
        )
        processStatistikkkategori(
            """
                {
                    "kode": "IT",
                    "kategori": "NÆRING",
                    "sistePubliserteKvartal": {
                        "prosent": 2.15,
                        "arstall": "2022",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 2.14,
                        "arstall": "2022",
                        "kvartal": "1"
                    }
                }
            """
        )


        mockMvc
            .perform(
                get("/api/sykefravaerstatistikk/{orgnr}", "123")
                    .with(jwtWithPid("42"))
            )
            .andExpect(status().isOk)
            .andReturn().response.contentAsString.also {
                assertEquals(
                    """
                    {
                        "type": "NÆRING",
                        "label": "IT",
                        "prosent": 3.14
                    }
                """, it, true
                )
            }
    }



    @Test
    fun `bruker med tilgang får statistikk for bransje når virksomhet mangler`() {
        `when`(
            altinnService.hentOrganisasjonerBasertPaRettigheter("42", "3403", "1")
        ).thenReturn(listOf(Organisasjon(organizationNumber = "123", name = "Foo & Co")))
        processMetadataVirksomhet(
            """
                {
                    "orgnr": "123",
                    "bransje": "Testing",
                    "naring": "IT",
                    "arstall": "2023",
                    "kvartal": "1"
                }
            """
        )
        processMetadataVirksomhet(
            """
                {
                    "orgnr": "123",
                    "bransje": "Testing Gammel",
                    "naring": "IT",
                    "arstall": "2022",
                    "kvartal": "1"
                }
            """
        )
        processStatistikkkategori(
            """
                {
                    "kode": "Testing",
                    "kategori": "BRANSJE",
                    "sistePubliserteKvartal": {
                        "prosent": 3.15,
                        "arstall": "2023",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 3.14,
                        "arstall": "2023",
                        "kvartal": "1"
                    }
                }
            """
        )
        processStatistikkkategori(
            """
                {
                    "kode": "Testing Gammel",
                    "kategori": "BRANSJE",
                    "sistePubliserteKvartal": {
                        "prosent": 2.15,
                        "arstall": "2022",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 2.14,
                        "arstall": "2022",
                        "kvartal": "1"
                    }
                }
            """
        )
        processStatistikkkategori(
            """
                {
                    "kode": "IT",
                    "kategori": "NÆRING",
                    "sistePubliserteKvartal": {
                        "prosent": 3.16,
                        "arstall": "2023",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 3.17,
                        "arstall": "2023",
                        "kvartal": "1"
                    }
                }
            """
        )
        processStatistikkkategori(
            """
                {
                    "kode": "IT Gammel",
                    "kategori": "NÆRING",
                    "sistePubliserteKvartal": {
                        "prosent": 2.16,
                        "arstall": "2022",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 2.17,
                        "arstall": "2022",
                        "kvartal": "1"
                    }
                }
            """
        )


        mockMvc
            .perform(
                get("/api/sykefravaerstatistikk/{orgnr}", "123")
                    .with(jwtWithPid("42"))
            )
            .andExpect(status().isOk)
            .andReturn().response.contentAsString.also {
                assertEquals(
                    """
                    {
                        "type": "BRANSJE",
                        "label": "Testing",
                        "prosent": 3.14
                    }
                """, it, true
                )
            }
    }

    @Test
    fun `no content dersom statistikk mangler`() {
        `when`(
            altinnService.hentOrganisasjonerBasertPaRettigheter("42", "3403", "1")
        ).thenReturn(listOf(Organisasjon(organizationNumber = "123", name = "Foo & Co")))
        processMetadataVirksomhet(
            """
                {
                    "orgnr": "123",
                    "bransje": "Testing",
                    "naring": "IT",
                    "arstall": "2023",
                    "kvartal": "1"
                }
            """
        )


        mockMvc
            .perform(
                get("/api/sykefravaerstatistikk/{orgnr}", "123")
                    .with(jwtWithPid("42"))
            )
            .andExpect(status().isNoContent)
    }

    private fun processStatistikkkategori(value: String) {
        sykefraværstatistikkKafkaListener.processStatistikkategori(
            ConsumerRecord(
                "", 0, 0, "", value
            )
        )
    }
    private fun processMetadataVirksomhet(value: String) {
        sykefraværstatistikkKafkaListener.processMetadataVirksomhet(
            ConsumerRecord(
                "", 0, 0, "", value
            )
        )
    }
}