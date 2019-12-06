package no.nav.tag.dittNavArbeidsgiver.controller;
import no.nav.security.oidc.api.Protected;
import no.nav.tag.dittNavArbeidsgiver.models.ArbeidsForhold;
import no.nav.tag.dittNavArbeidsgiver.models.OversiktOverArbeidsForhold;
import no.nav.tag.dittNavArbeidsgiver.services.aareg.AAregService;
import no.nav.tag.dittNavArbeidsgiver.services.pdl.PdlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

@Protected
@Slf4j
@RestController
public class AAregController {

    private final AAregService aAregServiceService;
    private final PdlService pdlService;

    @Autowired
    public AAregController(AAregService aAService, PdlService pdlService) {
        this.aAregServiceService = aAService;
        this.pdlService = pdlService;

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
}



