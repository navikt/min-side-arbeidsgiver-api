package no.nav.arbeidsgiver.min_side.services.tiltak;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Profile({"dev-gcp", "prod-gcp"})
@Slf4j
@Repository
public class RefusjonStatusRepositoryImpl implements RefusjonStatusRepository {

    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public RefusjonStatusRepositoryImpl(
            ObjectMapper objectMapper,
            JdbcTemplate jdbcTemplate,
            NamedParameterJdbcTemplate namedParameterJdbcTemplate
    ) {
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }


    @Profile({"dev-gcp","prod-gcp"})
    @KafkaListener(
            id = "min-side-arbeidsgiver-1",
            topics = "arbeidsgiver.tiltak-refusjon-endret-status",
            containerFactory = "errorLoggingKafkaListenerContainerFactory"
    )
    public void processConsumerRecord(ConsumerRecord<String, String> record) throws JsonProcessingException {
        RefusjonStatusHendelse hendelse = objectMapper.readValue(record.value(), RefusjonStatusHendelse.class);
        jdbcTemplate.update(
                "insert into refusjon_status(refusjon_id, virksomhetsnummer, avtale_id, status)" +
                        "   values(?, ?, ?, ?)" +
                        "   on conflict (refusjon_id) " +
                        "   do update set " +
                        "       virksomhetsnummer = EXCLUDED.virksomhetsnummer, " +
                        "       avtale_id = EXCLUDED.avtale_id, " +
                        "       status = EXCLUDED.status;",
                ps -> {
                    ps.setString(1, hendelse.refusjonId);
                    ps.setString(2, hendelse.virksomhetsnummer);
                    ps.setString(3, hendelse.avtaleId);
                    ps.setString(4, hendelse.status);
                }
        );
    }

    @Override
    public List<Statusoversikt> statusoversikt(List<String> virksomhetsnumre) {
        if (virksomhetsnumre.isEmpty()) {
            return List.of();
        }

        Map<String, Map<String, Integer>> grouped = namedParameterJdbcTemplate.queryForList(
                        "select virksomhetsnummer, status, count(*) as count " +
                                "from refusjon_status " +
                                "where virksomhetsnummer in (:virksomhetsnumre) " +
                                "group by virksomhetsnummer, status",
                        Map.of("virksomhetsnumre", virksomhetsnumre)
                )
                .stream()
                .collect(
                        groupingBy(
                                m -> (String) m.get("virksomhetsnummer"),
                                groupingBy(
                                        m -> (String) m.get("status"),
                                        Collectors.summingInt(m -> ((Long) m.get("count")).intValue())
                                )
                        ));

        return grouped.entrySet().stream()
                .map(entry -> new Statusoversikt(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }
}
