package no.nav.tag.dittNavArbeidsgiver.services.aktor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("dev")
public class AktorClientTest {

    @Autowired
    private AktorClient aktorClient;

    @Test
    public void getAktorId() {
        aktorClient.getAktorId("07045700172");
    }
}