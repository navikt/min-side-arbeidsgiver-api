package no.nav.security.tag.dittNavArbeidsgiver.controller;

import no.nav.tag.dittNavArbeidsgiver.services.altinn.AltinnService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OrganisasjonController {

    @InjectMocks
    private OrganisasjonController organisasjonController;

    @Mock
    private AltinnService altinnService;

    @Test
    public void hentOrganisasjoner__skal_returnere_ok_med_organisasjoner() {

    }
}
