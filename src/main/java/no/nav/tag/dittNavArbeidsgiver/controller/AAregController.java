package no.nav.tag.dittNavArbeidsgiver.controller;
import no.nav.security.oidc.api.Protected;
import no.nav.tag.dittNavArbeidsgiver.models.ArbeidsForhold;
import no.nav.tag.dittNavArbeidsgiver.models.OversiktArbeidsgiver;
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

import java.util.ArrayList;
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
        OversiktOverArbeidsForhold result = aAregServiceService.hentArbeidsforhold(orgnr,juridiskEnhetOrgnr,idToken);
        System.out.println("kommer hit");
        System.out.println("result.getArbeidsforholdoversikter().length: "+result.getArbeidsforholdoversikter().length);
        if (result.getArbeidsforholdoversikter().length>0) {
            System.out.println("result.getArbeidsforholdoversikter().length: "+result.getArbeidsforholdoversikter().length);
            for (ArbeidsForhold arbeidsforhold : result.getArbeidsforholdoversikter()) {
                String fnr = arbeidsforhold.getArbeidstaker().getOffentligIdent();
                String navn = pdlService.hentNavnMedFnr(fnr);
                arbeidsforhold.getArbeidstaker().setNavn(navn);
            }
        }
        System.out.println("result.getArbeidsforholdoversikter().length: "+result);
        return ResponseEntity.ok(result);
    }

    @GetMapping(value = "/api/arbeidsgivere")
    @ResponseBody
    public ResponseEntity<List<OversiktOverArbeidsgiver>> hentArbeidsgivere(@RequestHeader("orgnr") String orgnr, @RequestHeader("opplysningspliktig") String opplysningspliktig, @CookieValue("selvbetjening-idtoken") String idToken) {
        List<OversiktOverArbeidsgiver> result = aAregServiceService.hentArbeidsgiverefraRapporteringsplikig(orgnr,opplysningspliktig,idToken);
        System.out.println("kommer hit");
        System.out.println("result.getArbeidsforholdoversikter().length: "+result.size());
        if (result.size()>0) {
            System.out.println("result.getArbeidsforholdoversikter().length: "+ result.get(0));
            for (OversiktOverArbeidsgiver oversikt : result) {
                OversiktArbeidsgiver arbeidsgiver = oversikt.getArbeidsgiver();
                System.out.println("hentArbeidsgiver orgnummer: "+arbeidsgiver.getOrganisasjonsnummer());
            }
        }
        else{
            result = finnOpplysningspliktigorg(orgnr, idToken);
        }
        System.out.println("result.getArbeidsforholdoversikter().length: "+result);
        enhetsregisterService.hentOrgnaisasjonFraEnhetsregisteret(orgnr);
        return ResponseEntity.ok(result);
    }

    public List<OversiktOverArbeidsgiver> finnOpplysningspliktigorg(String orgnr, String idToken){
        System.out.println("finnOpplysningspliktigorg: orgnr" + orgnr);
        EnhetsRegisterOrg orgtreFraEnhetsregisteret = enhetsregisterService.hentOrgnaisasjonFraEnhetsregisteret(orgnr);
        if(orgtreFraEnhetsregisteret.getBestaarAvOrganisasjonsledd().size() > 0){
           return itererOverOrgTre(orgnr,orgtreFraEnhetsregisteret.getBestaarAvOrganisasjonsledd().get(0).getOrganisasjonsledd(), idToken );

        }
        return new ArrayList<OversiktOverArbeidsgiver>();
    }

    public List<OversiktOverArbeidsgiver> itererOverOrgTre(String orgnr, Organisasjoneledd orgledd, String idToken){
        System.out.println("itererOverOrgTre orgnr: " + orgnr);
        System.out.println("itererOverOrgTre orgledd: " + orgledd);
        List<OversiktOverArbeidsgiver> result = aAregServiceService.hentArbeidsgiverefraRapporteringsplikig(orgnr,orgledd.getOrganisasjonsnummer(),idToken);
        if(result.size() > 0){
            return result;
        }
        else if(orgledd.getInngaarIJuridiskEnheter().size()>0){
            String juridiskEnhetOrgnr = orgledd.getInngaarIJuridiskEnheter().get(0).getOrganisasjonsnummer();
            return aAregServiceService.hentArbeidsgiverefraRapporteringsplikig(orgnr,juridiskEnhetOrgnr,idToken);
        }
        else{
            return itererOverOrgTre(orgnr, orgledd.getOrganisasjonsleddOver().get(0).getOrganisasjonsledd(), idToken);
            }
        }
}



