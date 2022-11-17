package no.nav.arbeidsgiver.min_side.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeidsgiver.min_side.models.Organisasjon;
import no.nav.arbeidsgiver.min_side.services.digisyfo.*;
import no.nav.arbeidsgiver.min_side.services.ereg.EregService;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
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
    private final DigisyfoService digisyfoService;
    private final NærmestelederRepository nærmestelederRepository;
    private final SykmeldingRepository sykmeldingRepository;
    private final EregService eregService;
    private final AuthenticatedUserHolder authenticatedUserHolder;

    @Autowired
    public DigisyfoController(
            DigisyfoService digisyfoService,
            NærmestelederRepository nærmestelederRepository,
            SykmeldingRepository sykmeldingRepository,
            EregService eregService,
            AuthenticatedUserHolder authenticatedUserHolder
    ) {
        this.digisyfoService = digisyfoService;
        this.nærmestelederRepository = nærmestelederRepository;
        this.sykmeldingRepository = sykmeldingRepository;
        this.eregService = eregService;
        this.authenticatedUserHolder = authenticatedUserHolder;
    }


    @AllArgsConstructor
    @Data
    public static class VirksomhetOgAntallSykmeldte {
        final Organisasjon organisasjon;
        final int antallSykmeldte;
    }

    @GetMapping("/api/narmesteleder/virksomheter-v3")
    public Collection<VirksomhetOgAntallSykmeldte> hentVirksomheter() {
        String fnr = authenticatedUserHolder.getFnr();
        return digisyfoService.hentVirksomheterOgSykmeldte(fnr);
    }

    @AllArgsConstructor
    @Data
    static class DigisyfoOrganisasjonBakoverkompatibel {
        final Organisasjon organisasjon;
        final int antallSykmeldinger;
    }

    @GetMapping("/api/narmesteleder/virksomheter-v2")
    public List<DigisyfoOrganisasjonBakoverkompatibel> hentVirksomheterBakoverkompatibel() {
        String fnr = authenticatedUserHolder.getFnr();
        var aktiveSykmeldingerOversikt = sykmeldingRepository.oversiktSykmeldinger(fnr);
        Predicate<String> harAktiveSykmeldinger = virksomhetsnummer -> aktiveSykmeldingerOversikt.getOrDefault(virksomhetsnummer, 0) > 0;
        return nærmestelederRepository.virksomheterSomNærmesteLeder(fnr)
                .stream()
                .filter(harAktiveSykmeldinger)
                .flatMap(this::hentUnderenhetOgOverenhet)
                .filter(Objects::nonNull)
                .map(org -> new DigisyfoOrganisasjonBakoverkompatibel(
                        org,
                        aktiveSykmeldingerOversikt.getOrDefault(org.getOrganizationNumber(), 0)
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

