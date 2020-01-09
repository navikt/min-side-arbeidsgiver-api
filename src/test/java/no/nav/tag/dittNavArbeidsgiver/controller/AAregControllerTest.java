package no.nav.tag.dittNavArbeidsgiver.controller;

import no.nav.tag.dittNavArbeidsgiver.services.aareg.AAregService;
import no.nav.tag.dittNavArbeidsgiver.services.enhetsregisteret.EnhetsregisterService;
import no.nav.tag.dittNavArbeidsgiver.services.pdl.PdlService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@AutoConfigureMockMvc
@RunWith(SpringRunner.class)
@ActiveProfiles("dev")
public class AAregControllerTest {

    @Autowired
    private AAregService aAregService;

    @Mock
    private PdlService pdlService;

    @Autowired
    private EnhetsregisterService enhetsregisterService;

    @MockBean
    private AAregController aAregController;

    @Before
    public void setUp() {
        aAregController = new AAregController(aAregService,pdlService,enhetsregisterService);
    }

    @Test
    public void hentArbeidsforhold() {
        aAregController.hentArbeidsforhold("123","132","132");
    }

}