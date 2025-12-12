package no.nav.arbeidsgiver.min_side.services.digisyfo

import kotlinx.serialization.Serializable
import no.nav.arbeidsgiver.min_side.infrastruktur.*
import no.nav.arbeidsgiver.min_side.services.tiltak.RefusjonStatusRepository
import no.nav.arbeidsgiver.min_side.sykefravarstatistikk.SykefravarstatistikkRepository
import no.nav.arbeidsgiver.min_side.varslingstatus.VarslingStatusRepository
import kotlin.test.Test


class MsaKafkaConsumerImplTest {

    /**
     * In jackson, time modules are autodiscovered, but in kotlinx serialization it is explicit.
     * We can either register custom SerializersModule and use @Contextual annotations,
     * or we can create small wrapper types with custom serializers as shown below.
     * Explicit wrappers are easier to use, and does not require specific configuration of Json modules.
     * this test is not really needed, but kept for documentation purposes.
     */
    @Test
    fun `defaultJson handles localdate`() {
        @Serializable
        data class Wrapper(val dato: SerializableLocalDate)

        defaultJson.decodeFromString<Wrapper>("""{ "dato": "2020-01-01" }""")
    }


    @Test
    fun `sykmeldinger handles json items`() = testApplicationWithDatabase { db ->
        val processor = SykmeldingRecordProcessor(
            DigisyfoRepositoryImpl(db)
        )

        val fnr = "0011223344556"
        processor.processRecordsValue(
            """
                {
                    "sykmelding": {
                        "sykmeldingsperioder": [
                            { "tom": "2022-01-01" },
                            { "tom": "2022-03-01" }
                        ]
                    },
                    "kafkaMetadata": {
                        "fnr": "$fnr",
                        "not-used": 1
                    },
                    "event": {
                        "arbeidsgiver": {
                            "orgnummer": "112233445"
                        }
                    }
                }
            """
        )
    }

    @Test
    fun `nærmeste leder handles json`() = testApplicationWithDatabase { db ->
        val processor = NaermesteLederRecordProcessor(
            DigisyfoRepositoryImpl(db)
        )
        val naermesteLederFnr = "0011223344556"
        processor.processRecordValue(
            """{
                    "narmesteLederId": "20e8377b-9513-464a-8c09-4ebbd8c2b4e3",
                    "fnr":"***********",
                    "orgnummer":"974574861",
                    "narmesteLederFnr":"$naermesteLederFnr",
                    "narmesteLederTelefonnummer":"xxx",
                    "narmesteLederEpost":"xxx",
                    "aktivFom":"2020-02-24",
                    "aktivTom":"2020-02-24",
                    "arbeidsgiverForskutterer":true,
                    "timestamp":"2021-05-03T07:53:33.937472Z"
           }"""
        )
    }

    @Test
    fun `RefusjonStatusRecordProcessor handles json`() = testApplicationWithDatabase { db ->
        RefusjonStatusRecordProcessor(
            RefusjonStatusRepository(db)
        ).processRecordValue(
            """{
                    "refusjonId": "42",
                    "bedriftNr": "42",
                    "avtaleId": "314",
                    "status": "foobar",
                    "unknownField": "should be ignored"
            }"""
        )
    }

    @Test
    fun `SykefraværStatistikkMetadataRecordProcessor handles json`() = testApplicationWithDatabase { db ->
        SykefraværStatistikkMetadataRecordProcessor(
            SykefravarstatistikkRepository(db)
        ).processRecordKeyValue(
            """{ "orgnr": "123", "arstall": "2025", "kvartal": "1", "bah": "humbug" }""",
            """
                {
                    "orgnr": "123",
                    "bransje": "Testing",
                    "naring": "IT",
                    "arstall": "2025",
                    "kvartal": "1",
                    "unknownField": "should be ignored"
                }
            """
        )
    }

    @Test
    fun `SykefraværStatistikkRecordProcessor handles json`() = testApplicationWithDatabase { db ->
        SykefraværStatistikkRecordProcessor(
            SykefravarstatistikkRepository(db)
        ).processRecordKeyValue(
            """{ "kategori": "VIRKSOMHET", "kode": "123", "årstall": "2025", "kvartal": "1", "extra": "field" }""",
            """
                {
                    "kode": "123",
                    "kategori": "VIRKSOMHET",
                    "sistePubliserteKvartal": {
                        "prosent": 3.15,
                        "årstall": "2025",
                        "kvartal": "1"
                    },
                    "siste4Kvartal": {
                        "prosent": 3.14
                    },
                    "anotherUnknownField": "should be ignored"
                }
            """
        )
    }

    @Test
    fun `VarslingStatusRecordProcessor handles json`() = testApplicationWithDatabase { db ->
        VarslingStatusRecordProcessor(
            VarslingStatusRepository(db)
        ).processRecordValue("""
            {
                "virksomhetsnummer": "314",
                "varselId": "vid1",
                "varselTimestamp": "2021-01-01T00:00:00",
                "kvittertEventTimestamp": "2021-01-01T00:00:00Z",
                "status": "MANGLER_KOFUVI",
                "version": "1",
                "extraField": "should be ignored"
            }
        """.trimIndent())
    }
}
