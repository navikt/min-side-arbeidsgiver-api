package no.nav.tag.dittNavArbeidsgiver.services.pdl;

import no.nav.tag.dittNavArbeidsgiver.models.pdlBatch.PdlBatchRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import static org.junit.Assert.assertEquals;

@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("dev")
@TestPropertySource(properties = {"mock.port=8082"})
public class PdlBatchTester {
    @Autowired
    private PdlService pdlService;

    @Test
    public void lagRequest() {
        String[] fnrs = new String[]{"111111111","22222222"};
        PdlBatchRequest request =pdlService.getBatchFraPdltest(fnrs);
        assertEquals("query($identer: [ID!]!) { hentPersonBolk(identer: $identer) { ident, person { navn { fornavn mellomnavn etternavn } }, code } }",
                request.getQuery() );
    }

    @Test
    public void hentNavn() {
        String[] fnrs = new String[]{"111111111","22222222"};
        PdlBatchRequest request =pdlService.getBatchFraPdltest(fnrs);
        assertEquals("query($identer: [ID!]!) { hentPersonBolk(identer: $identer) { ident, person { navn { fornavn mellomnavn etternavn } }, code } }",
                request.getQuery() );
    }

}