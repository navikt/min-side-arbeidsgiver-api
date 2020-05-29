package no.nav.tag.dittNavArbeidsgiver.controller;
import no.nav.metrics.MetricsFactory;
import no.nav.metrics.Timer;
import no.nav.security.oidc.api.Protected;
import no.nav.tag.dittNavArbeidsgiver.config.ConcurrencyConfig;
import no.nav.tag.dittNavArbeidsgiver.models.ArbeidsForhold;
import no.nav.tag.dittNavArbeidsgiver.models.OversiktOverArbeidsForhold;
import no.nav.tag.dittNavArbeidsgiver.models.OversiktOverArbeidsgiver;
import no.nav.tag.dittNavArbeidsgiver.models.Yrkeskoderespons.Yrkeskoderespons;
import no.nav.tag.dittNavArbeidsgiver.models.enhetsregisteret.EnhetsRegisterOrg;
import no.nav.tag.dittNavArbeidsgiver.models.enhetsregisteret.Organisasjoneledd;
import no.nav.tag.dittNavArbeidsgiver.services.aareg.AAregService;
import no.nav.tag.dittNavArbeidsgiver.services.enhetsregisteret.EnhetsregisterService;
import no.nav.tag.dittNavArbeidsgiver.services.pdl.PdlService;
import no.nav.tag.dittNavArbeidsgiver.services.yrkeskode.KodeverkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import springfox.documentation.annotations.ApiIgnore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Protected
@Slf4j
@RestController
public class AAregController {

    private final AAregService aAregServiceService;
    private final PdlService pdlService;
    private final EnhetsregisterService enhetsregisterService;
    private final KodeverkService kodeverkService;
    private final ConcurrencyConfig concurrencyconfig;
    @Autowired
    public AAregController(AAregService aAService, PdlService pdlService, EnhetsregisterService enhetsregisterService, KodeverkService kodeverkService, ConcurrencyConfig concurrencyconfig) {
        this.aAregServiceService = aAService;
        this.pdlService = pdlService;
        this.enhetsregisterService = enhetsregisterService;
        this.kodeverkService = kodeverkService;
        this.concurrencyconfig = concurrencyconfig;
    }
    @GetMapping(value = "/api/arbeidsforhold")
    @ResponseBody
    public ResponseEntity<OversiktOverArbeidsForhold> hentArbeidsforhold(
            @RequestHeader("orgnr") String orgnr,
            @RequestHeader("jurenhet") String juridiskEnhetOrgnr,
            @ApiIgnore @CookieValue("selvbetjening-idtoken") String idToken) {

        //test hentkoder
        Yrkeskoderespons yrkeskodeBeskrivelser =kodeverkService.hentBetydningerAvYrkeskoder();
        log.info("MSA-AAREG hentet yrkeskoder + "+yrkeskodeBeskrivelser.getBetydninger().size());

        Timer timer = MetricsFactory.createTimer("DittNavArbeidsgiverApi.hentArbeidsforhold").start();
        log.info("MSA-AAREG controller hentArbeidsforhold orgnr: " + orgnr + " jurenhet: " + juridiskEnhetOrgnr );
        Timer kunArbeidstimer = MetricsFactory.createTimer("DittNavArbeidsgiverApi.kunArbeidsforhold").start();
        OversiktOverArbeidsForhold response = aAregServiceService.hentArbeidsforhold(orgnr,juridiskEnhetOrgnr,idToken);
        if (response.getArbeidsforholdoversikter()==null || response.getArbeidsforholdoversikter().length<=0) {
            log.info("MSA-AAREG controller hentArbeidsforhold fant ingen arbeidsforhold. Prøver å med overordnete enheter");
            response = finnOpplysningspliktigorg(orgnr, idToken);
        }
        log.info("MSA-AAREG controller hentArbeidsforhold fant arbeidsforhold: " + response.getArbeidsforholdoversikter().length);
        kunArbeidstimer.stop().report();
        OversiktOverArbeidsForhold arbeidsforholdMedNavn = settNavnPåArbeidsforhold(response);
        OversiktOverArbeidsForhold arbeidsforholdMedYrkesbeskrivelse = settYrkeskodebetydningPaAlleArbeidsforhold(arbeidsforholdMedNavn);
        timer.stop().report();
        return ResponseEntity.ok(arbeidsforholdMedYrkesbeskrivelse);
    }

    @GetMapping(value = "/api/arbeidsgivere")
    @ResponseBody
    public ResponseEntity<List<OversiktOverArbeidsgiver>> hentArbeidsgivere(@RequestHeader("orgnr") String orgnr, @RequestHeader("opplysningspliktig") String opplysningspliktig, @CookieValue("selvbetjening-idtoken") String idToken) {
        List<OversiktOverArbeidsgiver> result = aAregServiceService.hentArbeidsgiverefraRapporteringsplikig(orgnr,opplysningspliktig,idToken);
        return ResponseEntity.ok(result);
    }

    public OversiktOverArbeidsForhold finnOpplysningspliktigorg(String orgnr, String idToken){
        EnhetsRegisterOrg orgtreFraEnhetsregisteret = enhetsregisterService.hentOrgnaisasjonFraEnhetsregisteret(orgnr);
        log.info("MSA-AAREG finnOpplysningspliktigorg, orgtreFraEnhetsregisteret: " + orgtreFraEnhetsregisteret);
        if(orgtreFraEnhetsregisteret.getBestaarAvOrganisasjonsledd().size() > 0){

           return itererOverOrgtre(orgnr,orgtreFraEnhetsregisteret.getBestaarAvOrganisasjonsledd().get(0).getOrganisasjonsledd(), idToken );
        }
        return tomOversiktOverArbeidsforhold();
    }

    public OversiktOverArbeidsForhold itererOverOrgtre(String orgnr, Organisasjoneledd orgledd, String idToken){
        OversiktOverArbeidsForhold result = aAregServiceService.hentArbeidsforhold(orgnr,orgledd.getOrganisasjonsnummer(),idToken);
        log.info("MSA-AAREG itererOverOrgtre orgnr: " +orgnr + "orgledd: "+ orgledd);
        if(result.getArbeidsforholdoversikter()!=null&&result.getArbeidsforholdoversikter().length>0){
            return result;
        }
        else if(orgledd.getInngaarIJuridiskEnheter()!=null){
            String juridiskEnhetOrgnr = orgledd.getInngaarIJuridiskEnheter().get(0).getOrganisasjonsnummer();
            log.info("MSA-AAREG itererOverOrgtre orgnr: " +orgnr + "juridiskEnhetOrgnr: "+ juridiskEnhetOrgnr);
            OversiktOverArbeidsForhold juridiskenhetRespons = aAregServiceService.hentArbeidsforhold(orgnr,juridiskEnhetOrgnr,idToken);
            if(juridiskenhetRespons.getArbeidsforholdoversikter().length<=0||juridiskenhetRespons.getArbeidsforholdoversikter()==null){
                juridiskenhetRespons= tomOversiktOverArbeidsforhold();
            }
            return juridiskenhetRespons;
        }
        else{
            return itererOverOrgtre(orgnr, orgledd.getOrganisasjonsleddOver().get(0).getOrganisasjonsledd(), idToken);
        }
    }

    public OversiktOverArbeidsForhold settNavnPåArbeidsforhold (OversiktOverArbeidsForhold arbeidsforholdOversikt ) {
        log.info("MSA-AAREG hent navn på arbeidsforhold fra pdl");
        HashMap<String, CompletableFuture<String>> allFutures = new HashMap<>();
        Timer hentNavntimer = MetricsFactory.createTimer("DittNavArbeidsgiverApi.hentNavn").start();
        if (arbeidsforholdOversikt.getArbeidsforholdoversikter() != null) {
            for (ArbeidsForhold arbeidsforhold : arbeidsforholdOversikt.getArbeidsforholdoversikter()) {
                String fnr = arbeidsforhold.getArbeidstaker().getOffentligIdent();
                allFutures.put(fnr, CompletableFuture.supplyAsync(() -> {
                    return pdlService.hentNavnMedFnr(fnr);
                },concurrencyconfig.hentNavnExecutor()));
            }
            CompletableFuture.allOf(allFutures.values().toArray(new CompletableFuture[0]));
            for (ArbeidsForhold arbeidsforhold : arbeidsforholdOversikt.getArbeidsforholdoversikter()) {
                String fnr = arbeidsforhold.getArbeidstaker().getOffentligIdent();
                String navn = "Kunne ikke hente navn";
                try {
                    navn = allFutures.get(fnr).get();
                    arbeidsforhold.getArbeidstaker().setNavn(navn);
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    arbeidsforhold.getArbeidstaker().setNavn(navn);
                }
            }
        }

        hentNavntimer.stop().report();
        return arbeidsforholdOversikt;
    }

    public OversiktOverArbeidsForhold settYrkeskodebetydningPaAlleArbeidsforhold (OversiktOverArbeidsForhold arbeidsforholdOversikt) {
        Timer hentYrkerTimer = MetricsFactory.createTimer("DittNavArbeidsgiverApi.hentYrker").start();
        Yrkeskoderespons yrkeskodeBeskrivelser = kodeverkService.hentBetydningerAvYrkeskoder();
        if (arbeidsforholdOversikt.getArbeidsforholdoversikter() != null) {
            for (ArbeidsForhold arbeidsforhold : arbeidsforholdOversikt.getArbeidsforholdoversikter()) {
                String yrkeskode = arbeidsforhold.getYrke();
                String yrkeskodeBeskrivelse = finnYrkeskodebetydningPaYrke(yrkeskode, yrkeskodeBeskrivelser);
                arbeidsforhold.setYrkesbeskrivelse(yrkeskodeBeskrivelse);
            }
        }
        hentYrkerTimer.stop().report();
        return arbeidsforholdOversikt;
    }

    public String finnYrkeskodebetydningPaYrke(String yrkeskodenokkel, Yrkeskoderespons yrkeskoderespons) {
        return yrkeskoderespons.getBetydninger().get(yrkeskodenokkel).get(0).getBeskrivelser().getNb().getTekst();
    }
    public OversiktOverArbeidsForhold tomOversiktOverArbeidsforhold(){
        OversiktOverArbeidsForhold tomOversikt = new OversiktOverArbeidsForhold();
        ArbeidsForhold[] tomtarbeidsforholdArray ={};
        tomOversikt.setAntall(0);
        tomOversikt.setTotalAntall(0);
        tomOversikt.setArbeidsforholdoversikter(tomtarbeidsforholdArray);
        return tomOversikt;
    }
}