package no.nav.tag.dittNavArbeidsgiver.controller;

import no.nav.tag.dittNavArbeidsgiver.models.OversiktOverArbeidsForhold;
import no.nav.tag.dittNavArbeidsgiver.services.aareg.AAregService;
import no.nav.tag.dittNavArbeidsgiver.services.enhetsregisteret.EnhetsregisterService;
import no.nav.tag.dittNavArbeidsgiver.services.pdl.PdlService;
import no.nav.tag.dittNavArbeidsgiver.services.yrkeskode.KodeverkService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertThat;


@SpringBootTest
@AutoConfigureMockMvc
@RunWith(SpringRunner.class)
@ActiveProfiles("dev")
@TestPropertySource(properties = {"mock.port=8084"})
public class AAregControllerTest {

    @Autowired
    private AAregService aAregService;

    @Mock
    private PdlService pdlService;

    @Autowired
    private EnhetsregisterService enhetsregisterService;

    @Autowired
    private KodeverkService kodeverkService;

    @MockBean
    private AAregController aAregController;

    @Before
    public void setUp() {
        aAregController = new AAregController(aAregService,pdlService,enhetsregisterService, kodeverkService);
    }

    @Test
    public void hentArbeidsforhold() {
        ResponseEntity<OversiktOverArbeidsForhold> tomRespons= aAregController.hentArbeidsforhold("910825517","132","132");
        Assert.assertNull(tomRespons.getBody().getArbeidsforholdoversikter());
        ResponseEntity<OversiktOverArbeidsForhold> responsMedInnhold= aAregController.hentArbeidsforhold("910825518","132","132");
       Assert.assertEquals(13,responsMedInnhold.getBody().getArbeidsforholdoversikter().length );
    }
}