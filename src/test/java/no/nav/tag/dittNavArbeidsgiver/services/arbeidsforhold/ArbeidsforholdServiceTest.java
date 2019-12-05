package no.nav.tag.dittNavArbeidsgiver.services.arbeidsforhold;

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
public class ArbeidsforholdServiceTest {

    @Autowired
    private AAregService oversiktOverArbeidsForhold;

    @Test
    public void hentArbeidsforhold() {
        String result = oversiktOverArbeidsForhold.hentArbeidsforhold("07045700172","123","9999").getAktorIDtilArbeidstaker();
        assertEquals(result,"1442495989754");
    }
}


