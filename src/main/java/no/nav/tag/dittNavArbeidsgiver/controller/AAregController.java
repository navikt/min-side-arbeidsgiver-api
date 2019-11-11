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


    @GetMapping(value = "/api/arbeidsforhold/{orgnr}")
    @ResponseBody
    public ResponseEntity<OversiktOverArbeidsForhold> hentArbeidsforhold(@PathVariable String orgnr) {
        OversiktOverArbeidsForhold result = aAregServiceService.hentArbeidsforhold(orgnr);
        for (ArbeidsForhold arbeidsforhold : result.getArbeidsforholdoversikter()){
            String fnr = arbeidsforhold.getArbeidstaker().getOffentligIdent();
            String navn = pdlService.hentNavnMedFnr(fnr);
            arbeidsforhold.getArbeidstaker().setNavn(navn);
        }
        return ResponseEntity.ok(result);
    }
}



