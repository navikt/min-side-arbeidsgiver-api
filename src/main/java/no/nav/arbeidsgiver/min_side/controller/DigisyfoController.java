package no.nav.arbeidsgiver.min_side.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeidsgiver.min_side.models.Organisasjon;
import no.nav.arbeidsgiver.min_side.services.digisyfo.NærmestelederRepository;
import no.nav.arbeidsgiver.min_side.services.digisyfo.SykmeldingRepository;
import no.nav.arbeidsgiver.min_side.services.ereg.EregService;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder.*;


@ProtectedWithClaims(issuer = TOKENX, claimMap = {REQUIRED_LOGIN_LEVEL})
@RestController
@Slf4j
public class DigisyfoController {

    private final NærmestelederRepository nærmestelederRepository;
    private final SykmeldingRepository sykmeldingRepository;
    private final EregService eregService;
    private final AuthenticatedUserHolder authenticatedUserHolder;

    @Autowired
    public DigisyfoController(
            NærmestelederRepository nærmestelederRepository,
            SykmeldingRepository sykmeldingRepository,
            EregService eregService,
            AuthenticatedUserHolder authenticatedUserHolder
    ) {
        this.nærmestelederRepository = nærmestelederRepository;
        this.sykmeldingRepository = sykmeldingRepository;
        this.eregService = eregService;
        this.authenticatedUserHolder = authenticatedUserHolder;
    }

    @AllArgsConstructor
    @Data
    static class DigisyfoOrganisasjon {
        final Organisasjon organisasjon;
        final int antallSykmeldinger;
        final int antallSykmeldte;
    }

    @GetMapping("/api/narmesteleder/virksomheter-v2")
    public List<DigisyfoOrganisasjon> hentVirksomheterv2() {
        String fnr = authenticatedUserHolder.getFnr();
        var sykmeldingerOversikt = sykmeldingRepository.oversiktSykmeldinger(fnr);
        var sykmeldteOversikt = sykmeldingRepository.oversiktSykmeldte(fnr);
        Predicate<String> harAktiveSykmeldinger = virksomhetsnummer -> sykmeldingerOversikt.getOrDefault(virksomhetsnummer, 0) > 0;
        return nærmestelederRepository.virksomheterSomNærmesteLeder(fnr)
                .stream()
                .filter(harAktiveSykmeldinger)
                .flatMap(this::hentUnderenhetOgOverenhet)
                .filter(Objects::nonNull)
                .map(org -> new DigisyfoOrganisasjon(
                        org,
                        sykmeldingerOversikt.getOrDefault(org.getOrganizationNumber(), 0),
                        sykmeldteOversikt.getOrDefault(org.getOrganizationNumber(), 0)
                ))
                .collect(Collectors.toList());
    }

    Stream<Organisasjon> hentUnderenhetOgOverenhet(String virksomhetsnummer) {
        Organisasjon underenhet = eregService.hentUnderenhet(virksomhetsnummer);
        Organisasjon overenhet = null;
        if (underenhet != null) {
            overenhet = eregService.hentOverenhet(underenhet.getParentOrganizationNumber());
        }
        return Stream.of(underenhet, overenhet);
    }
}

