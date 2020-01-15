package no.nav.tag.dittNavArbeidsgiver.controller;
import no.nav.security.oidc.api.Protected;
import no.nav.tag.dittNavArbeidsgiver.models.ArbeidsForhold;
import no.nav.tag.dittNavArbeidsgiver.models.OversiktOverArbeidsForhold;
import no.nav.tag.dittNavArbeidsgiver.models.OversiktOverArbeidsgiver;
import no.nav.tag.dittNavArbeidsgiver.models.enhetsregisteret.EnhetsRegisterOrg;
import no.nav.tag.dittNavArbeidsgiver.models.enhetsregisteret.Organisasjoneledd;
import no.nav.tag.dittNavArbeidsgiver.services.aareg.AAregService;
import no.nav.tag.dittNavArbeidsgiver.services.enhetsregisteret.EnhetsregisterService;
import no.nav.tag.dittNavArbeidsgiver.services.pdl.PdlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Protected
@Slf4j
@RestController
public class AAregController {

    private final AAregService aAregServiceService;
    private final PdlService pdlService;
    private final EnhetsregisterService enhetsregisterService;

    @Autowired
    public AAregController(AAregService aAService, PdlService pdlService, EnhetsregisterService enhetsregisterService) {
        this.aAregServiceService = aAService;
        this.pdlService = pdlService;
        this.enhetsregisterService = enhetsregisterService;
    }

    @GetMapping(value = "/api/arbeidsforhold")
    @ResponseBody
    public ResponseEntity<OversiktOverArbeidsForhold> hentArbeidsforhold(@RequestHeader("orgnr") String orgnr, @RequestHeader("jurenhet") String juridiskEnhetOrgnr,@CookieValue("selvbetjening-idtoken") String idToken) {
        OversiktOverArbeidsForhold response = aAregServiceService.hentArbeidsforhold(orgnr,juridiskEnhetOrgnr,idToken);
        if (response.getArbeidsforholdoversikter()==null) {
            response = finnOpplysningspliktigorg(orgnr, idToken);
        }
        OversiktOverArbeidsForhold arbeidsforholdMedNavn = settNavnPåArbeidsforhold(response);
        return ResponseEntity.ok(arbeidsforholdMedNavn);
    }

    @GetMapping(value = "/api/arbeidsgivere")
    @ResponseBody
    public ResponseEntity<List<OversiktOverArbeidsgiver>> hentArbeidsgivere(@RequestHeader("orgnr") String orgnr, @RequestHeader("opplysningspliktig") String opplysningspliktig, @CookieValue("selvbetjening-idtoken") String idToken) {
        List<OversiktOverArbeidsgiver> result = aAregServiceService.hentArbeidsgiverefraRapporteringsplikig(orgnr,opplysningspliktig,idToken);
        return ResponseEntity.ok(result);
    }

    public OversiktOverArbeidsForhold finnOpplysningspliktigorg(String orgnr, String idToken){
        EnhetsRegisterOrg orgtreFraEnhetsregisteret = enhetsregisterService.hentOrgnaisasjonFraEnhetsregisteret(orgnr);
        if(orgtreFraEnhetsregisteret.getBestaarAvOrganisasjonsledd().size() > 0){
           return itererOverOrgtre(orgnr,orgtreFraEnhetsregisteret.getBestaarAvOrganisasjonsledd().get(0).getOrganisasjonsledd(), idToken );
        }
        return new OversiktOverArbeidsForhold();
    }

    public OversiktOverArbeidsForhold itererOverOrgtre(String orgnr, Organisasjoneledd orgledd, String idToken){
        OversiktOverArbeidsForhold result = aAregServiceService.hentArbeidsforhold(orgnr,orgledd.getOrganisasjonsnummer(),idToken);
        if(result.getArbeidsforholdoversikter()!=null){
            return result;
        }
        else if(orgledd.getInngaarIJuridiskEnheter()!=null){
            String juridiskEnhetOrgnr = orgledd.getInngaarIJuridiskEnheter().get(0).getOrganisasjonsnummer();
            return aAregServiceService.hentArbeidsforhold(orgnr,juridiskEnhetOrgnr,idToken);
        }
        else{
            return itererOverOrgtre(orgnr, orgledd.getOrganisasjonsleddOver().get(0).getOrganisasjonsledd(), idToken);
        }
    }

    public OversiktOverArbeidsForhold settNavnPåArbeidsforhold (OversiktOverArbeidsForhold arbeidsforholdOversikt ) {
        if (arbeidsforholdOversikt.getArbeidsforholdoversikter() != null) {
            for (ArbeidsForhold arbeidsforhold : arbeidsforholdOversikt.getArbeidsforholdoversikter()) {
                String fnr = arbeidsforhold.getArbeidstaker().getOffentligIdent();
                String navn = pdlService.hentNavnMedFnr(fnr);
                arbeidsforhold.getArbeidstaker().setNavn(navn);
            }
        }
        return arbeidsforholdOversikt;
    }
}



