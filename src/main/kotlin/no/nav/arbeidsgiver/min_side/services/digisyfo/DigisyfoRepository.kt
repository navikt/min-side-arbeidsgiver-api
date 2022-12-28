package no.nav.arbeidsgiver.min_side.services.digisyfo;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;

public interface DigisyfoRepository {
    @Data
    @AllArgsConstructor
    class Virksomhetsinfo {
        final String virksomhetsnummer;
        final Integer antallSykmeldte;
    }
    List<Virksomhetsinfo> virksomheterOgSykmeldte(String nærmestelederFnr);

    void processNærmesteLederEvent(NarmesteLederHendelse hendelse);
    void processSykmeldingEvent(List<ImmutablePair<String, SykmeldingHendelse>> records);
}
