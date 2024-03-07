package no.nav.arbeidsgiver.min_side.sykefraværstatistikk

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.math.BigDecimal

data class Statistikk(
    val kategori: String,
    val kode: String,
    val prosent: Double,
)

@Repository
class SykefraværstatistikkRepository(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {

    fun virksomhetstatistikk(virksomhetsnummer: String) = namedParameterJdbcTemplate.queryForList(
        """
        select kategori, kode, prosent
            from sykefraværstatistikk_v2
            where kategori = 'VIRKSOMHET' and kode = :virksomhetsnummer
            order by arstall desc, kvartal desc
        """.trimIndent(),
        mapOf("virksomhetsnummer" to virksomhetsnummer)
    ).firstOrNull()?.let {
        Statistikk(
            kategori = it["kategori"] as String,
            kode = it["kode"] as String,
            prosent = (it["prosent"] as BigDecimal).toDouble(),
        )
    }

    fun statistikk(virksomhetsnummer: String) = namedParameterJdbcTemplate.queryForList(
        """ -- TODO: vurder eksplisitte felt i stedet for coalesce her, kan by på problemer 
        select 
          coalesce(sb.kategori, sn.kategori) as kategori, 
          coalesce(sb.kode, sn.kode) as kode, 
          coalesce(sb.prosent, sn.prosent) as prosent
        from sykefraværstatistikk_metadata_v2 meta
          left join sykefraværstatistikk_v2 sb on (sb.kode = meta.bransje and sb.arstall = meta.arstall and sb.kvartal = meta.kvartal)
          left join sykefraværstatistikk_v2 sn on (sn.kode = meta.næring and sn.arstall = meta.arstall and sn.kvartal = meta.kvartal)
        where meta.virksomhetsnummer = :virksomhetsnummer
          and coalesce(sb.prosent, sn.prosent) is not null
        order by meta.arstall desc, meta.kvartal desc, kategori
        """.trimIndent(),
        mapOf("virksomhetsnummer" to virksomhetsnummer)
    ).firstOrNull()?.let {
        Statistikk(
            kategori = it["kategori"] as String,
            kode = it["kode"] as String,
            prosent = (it["prosent"] as BigDecimal).toDouble(),
        )
    }

    fun processMetadataVirksomhet(metadata: MetadataVirksomhetDto) {
        namedParameterJdbcTemplate.update(
            """
            insert into sykefraværstatistikk_metadata_v2(virksomhetsnummer, bransje, næring, arstall, kvartal) 
                values(:virksomhetsnummer, :bransje, :næring, :arstall, :kvartal)
            on conflict (virksomhetsnummer, arstall, kvartal) 
                do update set 
                    virksomhetsnummer = EXCLUDED.virksomhetsnummer,        
                    bransje = EXCLUDED.bransje,        
                    næring = EXCLUDED.næring;
            """.trimIndent(),
            mapOf(
                "virksomhetsnummer" to metadata.virksomhetsnummer,
                "bransje" to metadata.bransje,
                "næring" to metadata.næring,
                "arstall" to metadata.arstall,
                "kvartal" to metadata.kvartal,
            )
        )
    }

    fun processStatistikkategori(statistikkategori: StatistikkategoriDto) {
        namedParameterJdbcTemplate.update(
            """
            insert into sykefraværstatistikk_v2(kode, kategori, prosent, arstall, kvartal) 
                values(:kode, :kategori, :prosent, :arstall, :kvartal)
            on conflict (kode, arstall, kvartal) 
                do update set 
                    kode = EXCLUDED.kode,        
                    kategori = EXCLUDED.kategori,        
                    prosent = EXCLUDED.prosent;
            """.trimIndent(),
            mapOf(
                "kode" to statistikkategori.kode,
                "kategori" to statistikkategori.kategori,
                "prosent" to statistikkategori.prosent,
                "arstall" to statistikkategori.siste4Kvartal?.arstall,
                "kvartal" to statistikkategori.siste4Kvartal?.kvartal,
            )
        )
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
        id = "min-side-arbeidsgiver-sfmeta-2",
        topics = ["arbeidsgiver.sykefravarsstatistikk-metadata-virksomhet-v1"],
        containerFactory = "errorLoggingKafkaListenerContainerFactory"
    )
    fun processMetadataVirksomhet(record: ConsumerRecord<String?, String?>) =
        sykefraværstatistikkRepository.processMetadataVirksomhet(
            objectMapper.readValue(record.value(), MetadataVirksomhetDto::class.java)
        )

    @Profile("dev-gcp", "prod-gcp")
    @KafkaListener(
        id = "min-side-arbeidsgiver-sfstats-2",
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
    @param:JsonProperty("arstall") val arstall: Number,
    @param:JsonProperty("kvartal") val kvartal: Number,
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
        @param:JsonProperty("arstall") val arstall: Number,
        @param:JsonProperty("kvartal") val kvartal: Number,
    )
}
