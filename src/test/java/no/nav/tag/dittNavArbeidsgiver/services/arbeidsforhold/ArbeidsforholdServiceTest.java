package no.nav.tag.dittNavArbeidsgiver.services.arbeidsforhold;

import lombok.extern.slf4j.Slf4j;
import no.nav.tag.dittNavArbeidsgiver.controller.AAregController;
import no.nav.tag.dittNavArbeidsgiver.models.OversiktOverArbeidsForhold;
import no.nav.tag.dittNavArbeidsgiver.services.aareg.AAregService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import static org.junit.Assert.*;

@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("dev")
@TestPropertySource(properties = {"mock.port=8082"})

@Slf4j
public class ArbeidsforholdServiceTest {

    @Autowired
    private AAregService oversiktOverArbeidsForhold;

    @Autowired
    private AAregController oversiktOverArbeidsForholdController;

    @Test
    public void hentArbeidsforhold() {
        String result = oversiktOverArbeidsForhold.hentArbeidsforhold("910825518","983887457","9999").getAktorIDtilArbeidstaker();
        log.info(result);
        assertEquals("1157442896316",result);
    }

    @Test
    public void settNavn() {
        OversiktOverArbeidsForhold oversikt = oversiktOverArbeidsForhold.hentArbeidsforhold("910825518","983887457","9999");
        oversiktOverArbeidsForholdController.settNavnPåArbeidsforholdBatch(oversikt);
        assertEquals("Ola Normann",oversikt.getNavnTilArbeidstaker());
    }
}