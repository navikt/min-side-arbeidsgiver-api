package no.nav.tag.dittNavArbeidsgiver.controller.featureToggles;

import no.finn.unleash.Unleash;
import no.nav.security.oidc.api.Protected;
import org.springframework.beans.factory.annotation.Autowired;
        import org.springframework.http.HttpStatus;
        import org.springframework.http.ResponseEntity;
        import org.springframework.web.bind.annotation.GetMapping;
        import org.springframework.web.bind.annotation.RequestParam;
        import org.springframework.web.bind.annotation.RestController;
        import javax.servlet.http.HttpServletResponse;


        @Protected
@RestController
public class FeatureToggleController {
            private final Unleash unleash;

    @Autowired
    public FeatureToggleController(Unleash unleash) {
        this.unleash = unleash;
    }

    @GetMapping("api/feature")
    public ResponseEntity<Boolean> feature(
            @RequestParam("feature") String features,
            HttpServletResponse response
    ) {

        return ResponseEntity.status(HttpStatus.OK).body(unleash.isEnabled(features));

    }
}
