package no.nav.arbeidsgiver.min_side.services.digisyfo;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.arbeidsgiver.min_side.controller.DigisyfoController;
import no.nav.arbeidsgiver.min_side.models.Organisasjon;
import no.nav.arbeidsgiver.min_side.services.ereg.EregService;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class DigisyfoService {
    private final DigisyfoRepository digisyfoRepository;
    private final EregService eregService;
    private final MeterRegistry meterRegistry;

    public DigisyfoService(
            DigisyfoRepository digisyfoRepository,
            EregService eregService,
            MeterRegistry meterRegistry
    ) {
        this.digisyfoRepository = digisyfoRepository;
        this.eregService = eregService;
        this.meterRegistry = meterRegistry;
    }

    public Collection<DigisyfoController.VirksomhetOgAntallSykmeldte> hentVirksomheterOgSykmeldte(String fnr) {
        var underenheter = digisyfoRepository
                .virksomheterOgSykmeldte(fnr)
                .stream()
                .flatMap(this::hentUnderenhet)
                .collect(Collectors.toList());
        var overenheterOrgnr = underenheter
                .stream()
                .map(info -> info.getOrganisasjon().getParentOrganizationNumber())
                .collect(Collectors.toSet());
        var resultat = Stream.concat(
                underenheter.stream(),
                overenheterOrgnr
                        .stream()
                        .flatMap(this::hentOverenhet)
        ).collect(Collectors.toList());

        meterRegistry.counter(
                        "msa.digisyfo.tilgang",
                        "virksomheter",
                        Integer.toString(resultat.size())
                )
                .increment();

        return resultat;
    }

    private Stream<DigisyfoController.VirksomhetOgAntallSykmeldte> hentOverenhet(String orgnr) {
        Organisasjon hovedenhet = eregService.hentOverenhet(orgnr);
        if (hovedenhet == null) {
            return Stream.of();
        }
        return Stream.of(new DigisyfoController.VirksomhetOgAntallSykmeldte(hovedenhet, 0));
    }

    private Stream<DigisyfoController.VirksomhetOgAntallSykmeldte> hentUnderenhet(DigisyfoRepositoryImpl.Virksomhetsinfo virksomhetsinfo) {
        Organisasjon underenhet = eregService.hentUnderenhet(virksomhetsinfo.getVirksomhetsnummer());
        if (underenhet == null) {
            return Stream.of();
        }
        return Stream.of(new DigisyfoController.VirksomhetOgAntallSykmeldte(
                underenhet,
                virksomhetsinfo.getAntallSykmeldte()
        ));
    }
}
