package no.nav.arbeidsgiver.min_side.sykefraværstatistikk

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
import java.math.BigDecimal
import java.sql.PreparedStatement

data class Statistikk(
    val kategori: String,
    val kode: String,
    val prosent: Double,
)

@Repository
class SykefraværstatistikkRepository(
    private val jdbcTemplate: JdbcTemplate,
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {
    fun virksomhetstatistikk(virksomhetsnummer: String): Statistikk? {
        return namedParameterJdbcTemplate.queryForList(
            """
            select kategori, kode, prosent
                from sykefraværstatistikk
                where kategori = 'VIRKSOMHET' and kode = :virksomhetsnummer
            """.trimIndent(),
            mapOf("virksomhetsnummer" to virksomhetsnummer)
        ).firstOrNull()?.let {
            Statistikk(
                kategori = it["kategori"] as String,
                kode = it["kode"] as String,
                prosent = (it["prosent"] as BigDecimal).toDouble(),
            )
        }
    }

    fun statistikk(virksomhetsnummer: String): Statistikk? {
        return namedParameterJdbcTemplate.queryForList(
            """ -- TODO: vurder eksplisitte felt i stedet for coalesce her, kan by på problemer 
            select coalesce(sb.kategori, sn.kategori) as kategori, coalesce(sb.kode, sn.kode) as kode, coalesce(sb.prosent, sn.prosent) as prosent
                from sykefraværstatistikk_metadata meta
                left join sykefraværstatistikk sb on sb.kategori = 'BRANSJE' and sb.kode = meta.bransje
                left join sykefraværstatistikk sn on sn.kategori = 'NÆRING' and sn.kode = meta.næring
                where meta.virksomhetsnummer = :virksomhetsnummer
                and coalesce(sb.prosent, sn.prosent) is not null
                order by coalesce(sb.kategori, sn.kategori) 
            """.trimIndent(),
            mapOf("virksomhetsnummer" to virksomhetsnummer)
        ).firstOrNull()?.let {
            Statistikk(
                kategori = it["kategori"] as String,
                kode = it["kode"] as String,
                prosent = (it["prosent"] as BigDecimal).toDouble(),
            )
        }

    }

    fun processMetadataVirksomhet(metadata: MetadataVirksomhetDto) {
        jdbcTemplate.update(
            """
            insert into sykefraværstatistikk_metadata(virksomhetsnummer, bransje, næring) 
                values(?, ?, ?) 
            on conflict (virksomhetsnummer) 
                do update set 
                    virksomhetsnummer = EXCLUDED.virksomhetsnummer,        
                    bransje = EXCLUDED.bransje,        
                    næring = EXCLUDED.næring;
            """.trimIndent()
        ) { ps: PreparedStatement ->
            ps.setString(1, metadata.virksomhetsnummer)
            ps.setString(2, metadata.bransje)
            ps.setString(3, metadata.næring)
        }
    }

    fun processStatistikkategori(statistikkategori: StatistikkategoriDto) {
        if (!listOf("NÆRING", "BRANSJE", "VIRKSOMHET").contains(statistikkategori.kategori)) {
            return
        }

        jdbcTemplate.update(
            """
            insert into sykefraværstatistikk(kode, kategori, prosent) 
                values(?, ?, ?) 
            on conflict (kode) 
                do update set 
                    kode = EXCLUDED.kode,        
                    kategori = EXCLUDED.kategori,        
                    prosent = EXCLUDED.prosent;
            """.trimIndent()
        ) { ps: PreparedStatement ->
            ps.setString(1, statistikkategori.kode)
            ps.setString(2, statistikkategori.kategori)
            ps.setDouble(3, statistikkategori.prosent)
        }
    }

}

@Profile("dev-gcp", "prod-gcp")
@Service
class SykefraværstatistikkKafkaListener(
    private val sykefraværstatistikkRepository: SykefraværstatistikkRepository,
    private val objectMapper: ObjectMapper,
) {
    @Profile("dev-gcp", "prod-gcp")
    @KafkaListener(
        id = "min-side-arbeidsgiver-sfmeta-1",
        topics = ["arbeidsgiver.sykefravarsstatistikk-metadata-virksomhet-v1"],
        containerFactory = "errorLoggingKafkaListenerContainerFactory"
    )
    fun processMetadataVirksomhet(record: ConsumerRecord<String?, String?>) =
        sykefraværstatistikkRepository.processMetadataVirksomhet(
            objectMapper.readValue(record.value(), MetadataVirksomhetDto::class.java)
        )

    @Profile("dev-gcp", "prod-gcp")
    @KafkaListener(
        id = "min-side-arbeidsgiver-sfstats-1",
        topics = [
            "arbeidsgiver.sykefravarsstatistikk-virksomhet-v1",
            "arbeidsgiver.sykefravarsstatistikk-naring-v1",
            "arbeidsgiver.sykefravarsstatistikk-bransje-v1",
        ],
        containerFactory = "errorLoggingKafkaListenerContainerFactory"
    )
    fun processStatistikkategori(record: ConsumerRecord<String?, String?>) =
        sykefraværstatistikkRepository.processStatistikkategori(
            objectMapper.readValue(record.value(), StatistikkategoriDto::class.java)
        )
}

/**
 * dataminimert til det vi trenger
 * kilde: https://github.com/navikt/sykefravarsstatistikk-api/blob/master/src/main/java/no/nav/arbeidsgiver/sykefravarsstatistikk/api/infrastruktur/kafka/dto/MetadataVirksomhetKafkamelding.kt
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class MetadataVirksomhetDto @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) constructor(
    @param:JsonProperty("orgnr") val virksomhetsnummer: String,
    @param:JsonProperty("naring") val næring: String,
    @param:JsonProperty("bransje") val bransje: String?,
)

/**
 * dataminimert til det vi trenger
 * kilde: https://github.com/navikt/sykefravarsstatistikk-api/blob/master/src/main/java/no/nav/arbeidsgiver/sykefravarsstatistikk/api/infrastruktur/kafka/dto/StatistikkategoriKafkamelding.kt
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class StatistikkategoriDto @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) constructor(
    @param:JsonProperty("kategori") val kategori: String,
    @param:JsonProperty("kode") val kode: String, // orgnr dersom kategori er VIRKSOMHET, bransje dersom kategori er BRANSJE, osv
    @param:JsonProperty("siste4Kvartal") val siste4Kvartal: ProsentWrapper?,
) {
    val prosent: Double
        get() = siste4Kvartal?.prosent ?: 0.0

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ProsentWrapper @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) constructor(
        @param:JsonProperty("prosent") val prosent: Double,
    )
}
