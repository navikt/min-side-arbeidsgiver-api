package no.nav.arbeidsgiver.min_side.services.digisyfo;

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

    public DigisyfoService(DigisyfoRepository digisyfoRepository, EregService eregService) {
        this.digisyfoRepository = digisyfoRepository;
        this.eregService = eregService;
    }

    public Collection<DigisyfoController.VirksomhetOgAntallSykmeldte> hentVirksomheterOgSykmeldte(String fnr) {
        var underenheter = digisyfoRepository
                .sykmeldtePrVirksomhet(fnr)
                .stream()
                .flatMap(this::hentUnderenhet)
                .collect(Collectors.toList());
        var overenheterOrgnr = underenheter
                .stream()
                .map(info -> info.getOrganisasjon().getParentOrganizationNumber())
                .collect(Collectors.toSet());
        return Stream.concat(
                underenheter.stream(),
                overenheterOrgnr
                        .stream()
                        .flatMap(this::hentOverenhet)
        ).collect(Collectors.toList());
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
